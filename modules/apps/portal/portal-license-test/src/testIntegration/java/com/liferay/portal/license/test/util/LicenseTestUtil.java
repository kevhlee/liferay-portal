/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.license.test.util;

import com.liferay.petra.lang.CentralizedThreadLocal;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.events.EventsProcessorUtil;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.license.util.LicenseManagerUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.module.util.SystemBundleUtil;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.util.PortalClassLoaderUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.module.framework.ModuleFrameworkUtil;
import com.liferay.portal.util.LicenseUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import java.net.HttpURLConnection;
import java.net.URL;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jodd.io.FileUtil;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.launch.Framework;

/**
 * @author Tina Tian
 */
public class LicenseTestUtil {

	public static void deployFreeTierLicenseContent(
			String startDate, String expirationDate)
		throws Exception {

		StringBundler sb = new StringBundler(19);

		sb.append("<?xml version=\"1.0\"?>");
		sb.append("<license><account-name>");
		sb.append(_FREE_TIER_ACCOUNT_NAME);
		sb.append("</account-name><product-name>");
		sb.append(_FREE_TIER_PROCUT_NAME);
		sb.append("</product-name><product-version>2026.Q1</product-version>");
		sb.append("<license-type>");
		sb.append(_FREE_TIER_LICENSE_TYPE);
		sb.append("</license-type><license-version>6</license-version>");
		sb.append("<start-date>");
		sb.append(startDate);
		sb.append("</start-date><expiration-date>");
		sb.append(expirationDate);
		sb.append("</expiration-date>");
		sb.append("<max-cluster-nodes>3</max-cluster-nodes>");
		sb.append("<domains><domain>");
		sb.append(_FREE_TIER_DOMAIN);
		sb.append("</domain><domain>localhost</domain></domains>");
		sb.append("<key></key></license>");

		LicenseManagerUtil.registerLicense(
			JSONUtil.put("licenseXML", sb.toString()));
	}

	public static Set<String> getCurrentBundleNames() {
		Set<String> bundleNames = new HashSet<>();

		BundleContext bundleContext = SystemBundleUtil.getBundleContext();

		for (Bundle bundle : bundleContext.getBundles()) {
			bundleNames.add(bundle.getSymbolicName());
		}

		return bundleNames;
	}

	public static Map<String, String> getPortalLicenseProperties() {
		return LicenseManagerUtil.getLicenseProperties(_PRODUCT_ID_PORTAL);
	}

	public static String hitHomePage(String host, int port) throws Exception {
		URL url = new URL("http", host, port, StringPool.FORWARD_SLASH);

		HttpURLConnection httpURLConnection = null;

		try {
			httpURLConnection = (HttpURLConnection)url.openConnection();

			httpURLConnection.setConnectTimeout(0);
			httpURLConnection.setReadTimeout(0);
			httpURLConnection.setRequestMethod("GET");

			httpURLConnection.connect();

			ByteBuffer bytes = _read(httpURLConnection.getInputStream());

			return new String(
				bytes.array(), 0, bytes.limit(), StandardCharsets.UTF_8);
		}
		catch (IOException ioException1) {
			if (httpURLConnection == null) {
				throw ioException1;
			}

			try (InputStream inputStream = httpURLConnection.getErrorStream()) {
				if (inputStream != null) {
					while (inputStream.read() != -1);
				}
			}
			catch (IOException ioException2) {
				throw new IOException(ioException2);
			}

			throw ioException1;
		}
		finally {
			if (httpURLConnection != null) {
				httpURLConnection.disconnect();
			}
		}
	}

	public static boolean isReleaseBundle() {
		ClassLoader classLoader = PortalClassLoaderUtil.getClassLoader();

		try {
			classLoader.loadClass(
				"com.liferay.portal.ee.license.util.LicenseManagerHelper");

			return true;
		}
		catch (ReflectiveOperationException reflectiveOperationException) {
			if (_log.isDebugEnabled()) {
				_log.debug(reflectiveOperationException);
			}
		}

		return false;
	}

	public static void removeFreeTierLicense() {
		File binaryFile = _buildBinaryFile(
			_PRODUCT_ID_PORTAL, _FREE_TIER_ACCOUNT_NAME, _FREE_TIER_PROCUT_NAME,
			_FREE_TIER_LICENSE_TYPE);

		binaryFile.delete();

		LicenseManagerUtil.checkLicense(_PRODUCT_ID_PORTAL);
	}

	public static void removeAllLicenseBinaryFiles() throws Exception {
		File dir = new File(LicenseUtil.LICENSE_REPOSITORY_DIR);

		if (dir.exists()) {
			FileUtil.deleteDir(dir);
		}

		LicenseManagerUtil.checkLicense(_PRODUCT_ID_PORTAL);
		LicenseManagerUtil.checkLicense(_PRODUCT_ID_CMP);
	}

	public static void resetLifecycleAction() throws Exception {
		ClassLoader classLoader = PortalClassLoaderUtil.getClassLoader();

		Object lifecycleAction = ReflectionTestUtil.getFieldValue(
			EventsProcessorUtil.class, "_lifecycleAction");

		Class<?> clazz = lifecycleAction.getClass();

		Method installAndStartBundlesMethod = null;

		for (Method method : clazz.getDeclaredMethods()) {
			if (Arrays.equals(
					method.getParameterTypes(),
					new Class<?>[] {
						BundleContext.class, Map.class, Framework.class
					})) {

				method.setAccessible(true);

				installAndStartBundlesMethod = method;

				break;
			}
		}

		for (Field field : clazz.getDeclaredFields()) {
			if (Map.class.isAssignableFrom(field.getType())) {
				field.setAccessible(true);

				Object bundleData = field.get(lifecycleAction);

				if (bundleData != null) {
					installAndStartBundlesMethod.invoke(
						lifecycleAction, SystemBundleUtil.getBundleContext(),
						bundleData, ModuleFrameworkUtil.getFramework());
				}
			}
		}

		ReflectionTestUtil.setFieldValue(
			EventsProcessorUtil.class, "_lifecycleAction",
			ReflectionTestUtil.invoke(
				classLoader.loadClass(
					"com.liferay.portal.ee.license.util.LicenseManagerHelper"),
				"getLifecycleAction", new Class<?>[0]));
	}

	private static File _buildBinaryFile(
		String productId, String accountName, String productEntryName,
		String licenseType) {

		StringBundler sb = new StringBundler(6);

		if (productId.equals(_PRODUCT_ID_PORTAL)) {
			sb.append(StringUtil.extractChars(accountName));
			sb.append("_");
		}

		sb.append(StringUtil.extractChars(productEntryName));
		sb.append("_");
		sb.append(StringUtil.extractChars(licenseType));
		sb.append(".li");

		return new File(LicenseUtil.LICENSE_REPOSITORY_DIR, sb.toString());
	}

	private static ByteBuffer _read(InputStream inputStream) throws Exception {
		byte[] bytes = _bytes.get();

		int left = bytes.length;

		int length = -1;
		int offset = 0;

		while ((length = inputStream.read(bytes, offset, left)) != -1) {
			left -= length;
			offset += length;

			if (left == 0) {
				int newLength = bytes.length * 6 / 5;

				byte[] newBytes = new byte[newLength];

				System.arraycopy(bytes, 0, newBytes, 0, bytes.length);

				left = newLength - bytes.length;

				bytes = newBytes;

				_bytes.set(bytes);
			}
		}

		inputStream.close();

		return ByteBuffer.wrap(bytes, 0, offset);
	}

	private static final String _FREE_TIER_ACCOUNT_NAME = "Free Account";

	private static final String _FREE_TIER_DOMAIN = "free.tier.com";

	private static final String _FREE_TIER_LICENSE_TYPE = "free";

	private static final String _FREE_TIER_PROCUT_NAME = "DXP Production";

	private static final String _PRODUCT_ID_CMP =
		"f37efde7-11b1-6ad7-5c60-07bec3334db1";

	private static final String _PRODUCT_ID_PORTAL = "Portal";

	private static final Log _log = LogFactoryUtil.getLog(
		LicenseTestUtil.class);

	private static final ThreadLocal<byte[]> _bytes =
		CentralizedThreadLocal.withInitial(() -> new byte[8192]);

}
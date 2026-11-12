/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.license.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.petra.lang.SafeCloseable;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.ee.license.EntitlementManager;
import com.liferay.portal.kernel.events.LifecycleEvent;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.license.util.App;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.upload.FileItem;
import com.liferay.portal.kernel.upload.UploadServletRequest;
import com.liferay.portal.kernel.util.Constants;
import com.liferay.portal.kernel.util.DigesterUtil;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.Http;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.ProxyUtil;
import com.liferay.portal.kernel.util.Time;
import com.liferay.portal.upload.test.util.UploadTestUtil;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpSession;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import java.util.Collections;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * @author Kevin Lee
 */
@RunWith(Arquillian.class)
public class OfflineActivationEntitlementTest extends BaseEntitlementTestCase {

	@Test
	public void testInstallEntitlementZip() throws Exception {
		try (SafeCloseable safeCloseable1 = disableValidateWithSafeCloseable();
			SafeCloseable safeCloseable2 = resetLicenseDataWithSafeCloseble()) {

			String productId = getProductId(App.CMP);
			String productName = RandomTestUtil.randomString();

			String addOnContent = RandomTestUtil.randomString();

			String sha256Checksum = DigesterUtil.digestHex(
				DigesterUtil.SHA_256,
				ByteBuffer.wrap(addOnContent.getBytes(StandardCharsets.UTF_8)));

			String portalProductName = ENTERPRISE_PRODUCT_NAME_DXP;

			if (isCMS()) {
				portalProductName = ENTERPRISE_PRODUCT_NAME_CMS;
			}

			String licenseXML = StringBundler.concat(
				"<licenses>",
				buildEnterprisePortalLicenseXML(
					RandomTestUtil.randomString(), ENTERPRISE_LICENSE_TYPE, 0,
					portalProductName, Time.HOUR, "7",
					EntitlementManager.getInstallationId()),
				buildAppLicenseXML(
					App.CMP, System.currentTimeMillis(), Time.HOUR),
				"</licenses>");

			File file = _createEntitlementZipFile(
				createManifestJSON(
					licenseXML,
					JSONUtil.put(
						"productId", productId
					).put(
						"productName", productName
					).put(
						"sha256Checksum", sha256Checksum
					)),
				Collections.singletonMap(productId, addOnContent));

			File deployedFile = new File(
				PropsValues.AUTO_DEPLOY_DEPLOY_DIR, productId + ".lpkg");

			try {
				Assert.assertTrue(
					EntitlementManager.installEntitlementZip(file));

				Assert.assertArrayEquals(
					addOnContent.getBytes(StandardCharsets.UTF_8),
					FileUtil.getBytes(deployedFile));
			}
			finally {
				FileUtil.delete(file);
				FileUtil.delete(deployedFile);
			}

			assertEntitlementGood();
		}
	}

	@Test
	public void testInstallEntitlementZipWithAddOnChecksumMismatch()
		throws Exception {

		String productId = RandomTestUtil.randomString();
		String productName = RandomTestUtil.randomString();

		File file = _createEntitlementZipFile(
			createManifestJSON(
				null,
				JSONUtil.put(
					"productId", productId
				).put(
					"productName", productName
				).put(
					"sha256Checksum", RandomTestUtil.randomString()
				)),
			Collections.singletonMap(productId, RandomTestUtil.randomString()));

		try {
			Assert.assertFalse(EntitlementManager.installEntitlementZip(file));

			assertEntitlementOfflineInvalidFile();
		}
		finally {
			FileUtil.delete(file);
		}
	}

	@Test
	public void testInstallEntitlementZipWithCorruptedFile() throws Exception {
		File file = File.createTempFile("entitlement", ".zip");

		try {
			String content = RandomTestUtil.randomString();

			Files.write(file.toPath(), content.getBytes());

			Assert.assertFalse(EntitlementManager.installEntitlementZip(file));

			assertEntitlementOfflineInvalidFile();
		}
		finally {
			FileUtil.delete(file);
		}
	}

	@Test
	public void testInstallEntitlementZipWithDifferentInstallationId()
		throws Exception {

		try (SafeCloseable safeCloseable1 = disableValidateWithSafeCloseable();
			SafeCloseable safeCloseable2 = resetLicenseDataWithSafeCloseble()) {

			String portalProductName = ENTERPRISE_PRODUCT_NAME_DXP;

			if (isCMS()) {
				portalProductName = ENTERPRISE_PRODUCT_NAME_CMS;
			}

			String licenseXML = buildEnterprisePortalLicenseXML(
				RandomTestUtil.randomString(), ENTERPRISE_LICENSE_TYPE, 0,
				portalProductName, Time.HOUR, "7",
				RandomTestUtil.randomString());

			File file = _createEntitlementZipFile(
				createManifestJSON(licenseXML), Collections.emptyMap());

			try {
				Assert.assertFalse(
					EntitlementManager.installEntitlementZip(file));

				assertEntitlementNotActivated();
			}
			finally {
				FileUtil.delete(file);
			}
		}
	}

	@Test
	public void testInstallEntitlementZipWithoutAddOnEntry() throws Exception {
		String productId = RandomTestUtil.randomString();

		File file = _createEntitlementZipFile(
			createManifestJSON(null, JSONUtil.put("productId", productId)),
			Collections.emptyMap());

		try {
			Assert.assertFalse(EntitlementManager.installEntitlementZip(file));

			assertEntitlementOfflineInvalidFile();
		}
		finally {
			FileUtil.delete(file);
		}
	}

	@Test
	public void testInstallEntitlementZipWithoutManifestJSON()
		throws Exception {

		File file = _createZipFile(
			HashMapBuilder.put(
				"random.txt", RandomTestUtil.randomString()
			).build());

		try {
			Assert.assertFalse(EntitlementManager.installEntitlementZip(file));

			assertEntitlementOfflineInvalidFile();
		}
		finally {
			FileUtil.delete(file);
		}
	}

	@Test
	public void testUploadEntitlement() throws Exception {
		try (SafeCloseable safeCloseable1 = disableValidateWithSafeCloseable();
			SafeCloseable safeCloseable2 = resetLicenseDataWithSafeCloseble()) {

			String portalProductName = ENTERPRISE_PRODUCT_NAME_DXP;

			if (isCMS()) {
				portalProductName = ENTERPRISE_PRODUCT_NAME_CMS;
			}

			String licenseXML = buildEnterprisePortalLicenseXML(
				RandomTestUtil.randomString(), ENTERPRISE_LICENSE_TYPE, 0,
				portalProductName, Time.HOUR, "7",
				EntitlementManager.getInstallationId());

			File file = _createEntitlementZipFile(
				createManifestJSON(licenseXML), Collections.emptyMap());

			try {
				FileItem fileItem = ProxyUtil.newDelegateProxyInstance(
					FileItem.class.getClassLoader(), FileItem.class,
					new Object() {

						public void delete() {
						}

						public String getFileName() {
							return file.getName();
						}

						public File getStoreLocation() {
							return file;
						}

						public boolean isInMemory() {
							return false;
						}

					},
					null);

				MockHttpServletRequest mockHttpServletRequest =
					new MockHttpServletRequest();

				mockHttpServletRequest.setContentType("multipart/form-data");
				mockHttpServletRequest.setRequestURI(
					PortalUtil.getPathContext() +
						"/c/portal/license_activation");

				UploadServletRequest uploadServletRequest =
					UploadTestUtil.createUploadServletRequest(
						mockHttpServletRequest,
						Collections.singletonMap(
							"entitlementZIP", new FileItem[] {fileItem}),
						Collections.singletonMap(
							Constants.CMD,
							Collections.singletonList("uploadEntitlement")));

				MockHttpServletResponse mockHttpServletResponse =
					new MockHttpServletResponse();

				lifecycleAction.processLifecycleEvent(
					new LifecycleEvent(
						uploadServletRequest, mockHttpServletResponse));

				Assert.assertEquals(
					PortalUtil.getPathContext() +
						"/c/portal/license_activation_success",
					mockHttpServletResponse.getRedirectedUrl());

				assertEntitlementGood();

				String entitlementActivatedFlag =
					ReflectionTestUtil.getFieldValue(
						lifecycleAction, "_ENTITLEMENT_ACTIVATED");

				HttpSession httpSession = uploadServletRequest.getSession();

				Assert.assertEquals(
					Boolean.TRUE,
					httpSession.getAttribute(entitlementActivatedFlag));
			}
			finally {
				FileUtil.delete(file);
			}
		}
	}

	@Test
	public void testUploadEntitlementSuccessPageViewedOnce() throws Exception {
		try (SafeCloseable safeCloseable1 = disableValidateWithSafeCloseable();
			SafeCloseable safeCloseable2 = resetLicenseDataWithSafeCloseble()) {

			String portalProductName = ENTERPRISE_PRODUCT_NAME_DXP;

			if (isCMS()) {
				portalProductName = ENTERPRISE_PRODUCT_NAME_CMS;
			}

			String licenseXML = buildEnterprisePortalLicenseXML(
				RandomTestUtil.randomString(), ENTERPRISE_LICENSE_TYPE, 0,
				portalProductName, Time.HOUR, "7",
				EntitlementManager.getInstallationId());

			File file = _createEntitlementZipFile(
				createManifestJSON(licenseXML), Collections.emptyMap());

			try {
				String boundary = RandomTestUtil.randomString();

				Http.Options options1 = new Http.Options();

				options1.addFilePart(
					"entitlementZIP", file.getName(), FileUtil.getBytes(file),
					"application/zip", StringPool.UTF8);
				options1.addHeader(
					"Content-Type",
					"multipart/form-data; boundary=" + boundary);
				options1.addPart(Constants.CMD, "uploadEntitlement");
				options1.setCookies(
					new Cookie[] {new Cookie("_unused", StringPool.BLANK)});
				options1.setCookieSpec(Http.CookieSpec.STANDARD);
				options1.setLocation(
					StringBundler.concat(
						"http://localhost:", getLocalPort(),
						PortalUtil.getPathContext(),
						"/c/portal/license_activation"));
				options1.setMethod(Http.Method.POST);

				HttpUtil.URLtoString(options1);

				assertEntitlementGood();

				Http.Options options2 = new Http.Options();

				options2.setCookies(HttpUtil.getCookies());
				options2.setCookieSpec(Http.CookieSpec.STANDARD);
				options2.setLocation(
					"http://localhost:" + getLocalPort() +
						PortalUtil.getPathContext());

				String response2 = HttpUtil.URLtoString(options2);

				Assert.assertFalse(response2.contains(SUCCESS_PAGE_KEY));
			}
			finally {
				FileUtil.delete(file);
			}
		}
	}

	private File _createEntitlementZipFile(
			String manifestJSON, Map<String, String> addOnProductIdContents)
		throws IOException {

		Map<String, String> entries = HashMapBuilder.put(
			"manifest.json", manifestJSON
		).build();

		for (Map.Entry<String, String> addOnProductIdContent :
				addOnProductIdContents.entrySet()) {

			entries.put(
				"add-ons/" + addOnProductIdContent.getKey() + ".lpkg",
				addOnProductIdContent.getValue());
		}

		return _createZipFile(entries);
	}

	private File _createZipFile(Map<String, String> entries)
		throws IOException {

		File file = File.createTempFile("entitlement", ".zip");

		try (ZipOutputStream zipOutputStream = new ZipOutputStream(
				new FileOutputStream(file))) {

			for (Map.Entry<String, String> entry : entries.entrySet()) {
				String value = entry.getValue();

				zipOutputStream.putNextEntry(new ZipEntry(entry.getKey()));
				zipOutputStream.write(value.getBytes(StandardCharsets.UTF_8));
				zipOutputStream.closeEntry();
			}
		}

		return file;
	}

}
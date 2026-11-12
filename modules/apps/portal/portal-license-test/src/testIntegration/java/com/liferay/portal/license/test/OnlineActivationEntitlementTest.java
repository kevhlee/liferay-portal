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
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.license.util.App;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.util.Constants;
import com.liferay.portal.kernel.util.DigesterUtil;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.Http;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.Time;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import java.net.InetSocketAddress;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.Signature;

import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Kevin Lee
 */
@RunWith(Arquillian.class)
public class OnlineActivationEntitlementTest extends BaseEntitlementTestCase {

	@Test
	public void testActivateInstallationSuccessPageViewedOnce()
		throws Exception {

		try (SafeCloseable safeCloseable1 = disableValidateWithSafeCloseable();
			SafeCloseable safeCloseable2 = resetLicenseDataWithSafeCloseble();
			MockOneLiferayHttpServer mockOneLiferayHttpServer =
				new MockOneLiferayHttpServer()) {

			mockOneLiferayHttpServer.setEntitlement();

			Http.Options options1 = new Http.Options();

			options1.addPart(Constants.CMD, "activateInstallation");
			options1.addPart(
				"activationCode",
				mockOneLiferayHttpServer.getValidActivationCode());
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
	}

	@Test
	public void testInstallEntitlementOnline() throws Exception {
		try (SafeCloseable safeCloseable1 = disableValidateWithSafeCloseable();
			SafeCloseable safeCloseable2 = resetLicenseDataWithSafeCloseble();
			MockOneLiferayHttpServer mockOneLiferayHttpServer =
				new MockOneLiferayHttpServer()) {

			mockOneLiferayHttpServer.offerAddOn(App.CMP);
			mockOneLiferayHttpServer.grantAppLicense(App.CMP);

			String productId = getProductId(App.CMP);

			File deployedFile = new File(
				PropsValues.AUTO_DEPLOY_DEPLOY_DIR, productId + ".lpkg");

			try {
				Assert.assertTrue(
					EntitlementManager.installEntitlementOnline(1));

				Assert.assertArrayEquals(
					mockOneLiferayHttpServer.getAddOnContent(App.CMP),
					FileUtil.getBytes(deployedFile));
			}
			finally {
				FileUtil.delete(deployedFile);
			}

			assertEntitlementGood();
		}
	}

	@Test
	public void testInstallEntitlementOnlineWithAddOnChecksumMismatch()
		throws Exception {

		try (MockOneLiferayHttpServer mockOneLiferayHttpServer =
				new MockOneLiferayHttpServer()) {

			mockOneLiferayHttpServer.offerAddOn(App.CMP);
			mockOneLiferayHttpServer.corruptAddOnChecksum(App.CMP);

			Assert.assertFalse(EntitlementManager.installEntitlementOnline(1));

			assertEntitlementOnlineUnavailable();
		}
	}

	@Test
	public void testInstallEntitlementOnlineWithAddOnDownloadExhaustedRetries()
		throws Exception {

		try (MockOneLiferayHttpServer mockOneLiferayHttpServer =
				new MockOneLiferayHttpServer()) {

			mockOneLiferayHttpServer.offerAddOn(App.CMP);
			mockOneLiferayHttpServer.setAddOnDownloadFailureCount(App.CMP, 2);

			Assert.assertFalse(EntitlementManager.installEntitlementOnline(1));

			assertEntitlementOnlineUnavailable();
		}
	}

	@Test
	public void testInstallEntitlementOnlineWithAddOnDownloadRetrySuccess()
		throws Exception {

		try (SafeCloseable safeCloseable1 = disableValidateWithSafeCloseable();
			SafeCloseable safeCloseable2 = resetLicenseDataWithSafeCloseble();
			MockOneLiferayHttpServer mockOneLiferayHttpServer =
				new MockOneLiferayHttpServer()) {

			mockOneLiferayHttpServer.offerAddOn(App.CMP);
			mockOneLiferayHttpServer.grantAppLicense(App.CMP);
			mockOneLiferayHttpServer.setAddOnDownloadFailureCount(App.CMP, 1);

			String productId = getProductId(App.CMP);

			File deployedFile = new File(
				PropsValues.AUTO_DEPLOY_DEPLOY_DIR, productId + ".lpkg");

			try {
				Assert.assertTrue(
					EntitlementManager.installEntitlementOnline(1));

				Assert.assertArrayEquals(
					mockOneLiferayHttpServer.getAddOnContent(App.CMP),
					FileUtil.getBytes(deployedFile));
			}
			finally {
				FileUtil.delete(deployedFile);
			}

			assertEntitlementGood();
		}
	}

	@Test
	public void testInstallEntitlementOnlineWithInvalidProductLicense()
		throws Exception {

		try (SafeCloseable safeCloseable1 = disableValidateWithSafeCloseable();
			SafeCloseable safeCloseable2 = resetLicenseDataWithSafeCloseble();
			MockOneLiferayHttpServer mockOneLiferayHttpServer =
				new MockOneLiferayHttpServer()) {

			mockOneLiferayHttpServer.offerAddOn(App.CMP);

			String productId = getProductId(App.CMP);

			File deployedFile = new File(
				PropsValues.AUTO_DEPLOY_DEPLOY_DIR, productId + ".lpkg");

			try {
				Assert.assertFalse(
					EntitlementManager.installEntitlementOnline(1));

				assertEntitlementNotActivated();
			}
			finally {
				FileUtil.delete(deployedFile);
			}
		}
	}

	@Test
	public void testInstallEntitlementOnlineWithManifestFetchFailure()
		throws Exception {

		try (MockOneLiferayHttpServer mockOneLiferayHttpServer =
				new MockOneLiferayHttpServer()) {

			Assert.assertFalse(EntitlementManager.installEntitlementOnline(1));

			assertEntitlementOnlineUnavailable();
		}
	}

	@Test
	public void testInstallEntitlementOnlineWithoutAddOns() throws Exception {
		try (SafeCloseable safeCloseable1 = disableValidateWithSafeCloseable();
			SafeCloseable safeCloseable2 = resetLicenseDataWithSafeCloseble();
			MockOneLiferayHttpServer mockOneLiferayHttpServer =
				new MockOneLiferayHttpServer()) {

			mockOneLiferayHttpServer.setEntitlement();

			Assert.assertTrue(EntitlementManager.installEntitlementOnline(1));

			assertEntitlementGood();
		}
	}

	@Test
	public void testRegisterInstallation() throws Exception {
		try (MockOneLiferayHttpServer mockOneLiferayHttpServer =
				new MockOneLiferayHttpServer()) {

			Assert.assertTrue(
				EntitlementManager.registerInstallation(
					mockOneLiferayHttpServer.getValidActivationCode()));

			assertEntitlementGood();
		}
	}

	@Test
	public void testRegisterInstallationWithInvalidActivationCode()
		throws Exception {

		try (MockOneLiferayHttpServer mockOneLiferayHttpServer =
				new MockOneLiferayHttpServer()) {

			Assert.assertFalse(
				EntitlementManager.registerInstallation(
					RandomTestUtil.randomString()));

			assertEntitlementOnlineInvalidActivationCode();
		}
	}

	@Test
	public void testRegisterInstallationWithNoConnection() throws Exception {
		String url;

		try (MockOneLiferayHttpServer mockOneLiferayHttpServer =
				new MockOneLiferayHttpServer()) {

			url = mockOneLiferayHttpServer.getURL();
		}

		try (AutoCloseable autoCloseable =
				ReflectionTestUtil.setFieldValueWithAutoCloseable(
					EntitlementManager.class, "_oneLiferayLocation", url)) {

			Assert.assertFalse(
				EntitlementManager.registerInstallation(
					RandomTestUtil.randomString()));

			assertEntitlementOnlineNoConnection();
		}
	}

	private static class MockOneLiferayHttpServer implements AutoCloseable {

		public MockOneLiferayHttpServer() throws IOException {
			_httpServer = HttpServer.create(
				new InetSocketAddress("127.0.0.1", 0), 0);

			_httpServer.start();

			_oneLiferayLocationAutoCloseable =
				ReflectionTestUtil.setFieldValueWithAutoCloseable(
					EntitlementManager.class, "_oneLiferayLocation", getURL());

			_httpServer.createContext(
				"/dxp/environments/activation",
				httpExchange -> {
					JSONObject jsonObject = _getJWTPayloadJSONObject(
						FileUtil.getBytes(httpExchange.getRequestBody()));

					int statusCode = HttpServletResponse.SC_FORBIDDEN;

					if (Objects.equals(
							jsonObject.getString("activationCode"),
							_validActivationCode)) {

						statusCode = HttpServletResponse.SC_OK;
					}

					_sendResponse(httpExchange, statusCode, new byte[0]);
				});
		}

		@Override
		public void close() throws Exception {
			_httpServer.stop(0);

			_oneLiferayLocationAutoCloseable.close();
		}

		public void corruptAddOnChecksum(App app) {
			_corruptedAddOnProductIds.add(getProductId(app));
		}

		public byte[] getAddOnContent(App app) {
			return _addOnContents.get(getProductId(app));
		}

		public String getAddOnDownloadURL(String productId) {
			return getURL() + "/add-ons/" + productId;
		}

		public String getURL() {
			InetSocketAddress inetSocketAddress = _httpServer.getAddress();

			return "http://127.0.0.1:" + inetSocketAddress.getPort();
		}

		public String getValidActivationCode() {
			return _validActivationCode;
		}

		public void grantAppLicense(App app) {
			_grantedApps.add(app);

			setEntitlement();
		}

		public void offerAddOn(App app) {
			String productId = getProductId(app);

			_addOnContents.put(
				productId,
				RandomTestUtil.randomString(
				).getBytes(
					StandardCharsets.UTF_8
				));

			_httpServer.createContext(
				"/add-ons/" + productId,
				httpExchange -> _handleAddOnDownload(httpExchange, productId));

			setEntitlement();
		}

		public void setAddOnDownloadFailureCount(App app, int failureCount) {
			_addOnDownloadFailureCounts.put(
				getProductId(app), new AtomicInteger(failureCount));
		}

		public void setEntitlement() {
			if (_manifestContextRegistered) {
				return;
			}

			_manifestContextRegistered = true;

			_httpServer.createContext(
				"/dxp/environments/manifest",
				httpExchange -> {
					_getSignedJWTPayloadJSONObject(
						FileUtil.getBytes(httpExchange.getRequestBody()));

					_sendResponse(
						httpExchange, HttpServletResponse.SC_OK,
						_buildManifestJSON().getBytes(StandardCharsets.UTF_8));
				});
		}

		private String _buildLicenseXML() {
			String portalProductName = ENTERPRISE_PRODUCT_NAME_DXP;

			if (isCMS()) {
				portalProductName = ENTERPRISE_PRODUCT_NAME_CMS;
			}

			String licenseXML = buildEnterprisePortalLicenseXML(
				RandomTestUtil.randomString(), ENTERPRISE_LICENSE_TYPE, 0,
				portalProductName, Time.HOUR, "7",
				EntitlementManager.getInstallationId());

			if (_grantedApps.isEmpty()) {
				return licenseXML;
			}

			StringBundler sb = new StringBundler();

			sb.append("<licenses>");
			sb.append(licenseXML);

			for (App app : _grantedApps) {
				sb.append(
					buildAppLicenseXML(
						app, System.currentTimeMillis(), Time.HOUR));
			}

			sb.append("</licenses>");

			return sb.toString();
		}

		private String _buildManifestJSON() {
			JSONArray addOnsJSONArray = JSONFactoryUtil.createJSONArray();

			for (Map.Entry<String, byte[]> entry : _addOnContents.entrySet()) {
				String productId = entry.getKey();

				String sha256Checksum = DigesterUtil.digestHex(
					DigesterUtil.SHA_256, ByteBuffer.wrap(entry.getValue()));

				if (_corruptedAddOnProductIds.contains(productId)) {
					sha256Checksum = RandomTestUtil.randomString();
				}

				addOnsJSONArray.put(
					JSONUtil.put(
						"downloadURL", getAddOnDownloadURL(productId)
					).put(
						"productId", productId
					).put(
						"productName", productId
					).put(
						"sha256Checksum", sha256Checksum
					));
			}

			Base64.Encoder encoder = Base64.getEncoder();

			return JSONUtil.put(
				"add-ons", addOnsJSONArray
			).put(
				"licenseXML",
				encoder.encodeToString(
					_buildLicenseXML().getBytes(StandardCharsets.UTF_8))
			).toString();
		}

		private JSONObject _getJWTPayloadJSONObject(byte[] requestBody)
			throws IOException {

			String[] parts = new String(
				requestBody, StandardCharsets.UTF_8
			).split(
				"\\.", -1
			);

			try {
				Base64.Decoder decoder = Base64.getUrlDecoder();

				return JSONFactoryUtil.createJSONObject(
					new String(
						decoder.decode(parts[1]), StandardCharsets.UTF_8));
			}
			catch (JSONException jsonException) {
				throw new IOException(jsonException);
			}
		}

		private PublicKey _getPublicKey() throws IOException {
			try {
				return ReflectionTestUtil.invoke(
					EntitlementManager.class, "_getPublicKey", new Class<?>[0]);
			}
			catch (Exception exception) {
				throw new IOException(exception);
			}
		}

		private JSONObject _getSignedJWTPayloadJSONObject(byte[] requestBody)
			throws IOException {

			String[] parts = new String(
				requestBody, StandardCharsets.UTF_8
			).split(
				"\\.", -1
			);

			try {
				Signature signature = Signature.getInstance("SHA256withRSA");

				signature.initVerify(_getPublicKey());

				String data = parts[0] + "." + parts[1];

				signature.update(data.getBytes(StandardCharsets.UTF_8));

				Base64.Decoder decoder = Base64.getUrlDecoder();

				Assert.assertTrue(signature.verify(decoder.decode(parts[2])));

				return JSONFactoryUtil.createJSONObject(
					new String(
						decoder.decode(parts[1]), StandardCharsets.UTF_8));
			}
			catch (GeneralSecurityException | JSONException exception) {
				throw new IOException(exception);
			}
		}

		private void _handleAddOnDownload(
				HttpExchange httpExchange, String productId)
			throws IOException {

			AtomicInteger failureCount = _addOnDownloadFailureCounts.get(
				productId);

			if ((failureCount != null) &&
				(failureCount.getAndDecrement() > 0)) {

				httpExchange.close();

				return;
			}

			_getSignedJWTPayloadJSONObject(
				FileUtil.getBytes(httpExchange.getRequestBody()));

			_sendResponse(
				httpExchange, HttpServletResponse.SC_OK,
				_addOnContents.get(productId));
		}

		private void _sendResponse(
				HttpExchange httpExchange, int statusCode, byte[] body)
			throws IOException {

			httpExchange.sendResponseHeaders(statusCode, body.length);

			try (OutputStream outputStream = httpExchange.getResponseBody()) {
				outputStream.write(body);
			}
		}

		private final Map<String, byte[]> _addOnContents = new HashMap<>();
		private final Map<String, AtomicInteger> _addOnDownloadFailureCounts =
			new HashMap<>();
		private final Set<String> _corruptedAddOnProductIds = new HashSet<>();
		private final Set<App> _grantedApps = new HashSet<>();
		private final HttpServer _httpServer;
		private boolean _manifestContextRegistered;
		private final AutoCloseable _oneLiferayLocationAutoCloseable;
		private final String _validActivationCode =
			RandomTestUtil.randomString();

	}

}
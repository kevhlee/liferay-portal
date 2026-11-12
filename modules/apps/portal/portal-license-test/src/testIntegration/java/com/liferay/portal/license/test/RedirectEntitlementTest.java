/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.license.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.petra.lang.SafeCloseable;
import com.liferay.portal.ee.license.EntitlementConstants;
import com.liferay.portal.ee.license.EntitlementManager;
import com.liferay.portal.kernel.events.LifecycleEvent;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.Time;

import jakarta.servlet.http.HttpSession;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * @author Kevin Lee
 */
@RunWith(Arquillian.class)
public class RedirectEntitlementTest extends BaseEntitlementTestCase {

	@Test
	public void testGetRedirectForLicenseActivationSuccessWithSessionFlag()
		throws Exception {

		String entitlementActivatedKey = ReflectionTestUtil.getFieldValue(
			lifecycleAction, "_ENTITLEMENT_ACTIVATED");

		MockHttpServletRequest mockHttpServletRequest1 =
			new MockHttpServletRequest();

		mockHttpServletRequest1.setRequestURI(
			PortalUtil.getPathContext() +
				"/c/portal/license_activation_success");

		HttpSession httpSession = mockHttpServletRequest1.getSession();

		httpSession.setAttribute(entitlementActivatedKey, Boolean.TRUE);

		MockHttpServletResponse mockHttpServletResponse1 =
			new MockHttpServletResponse();

		lifecycleAction.processLifecycleEvent(
			new LifecycleEvent(
				mockHttpServletRequest1, mockHttpServletResponse1));

		Assert.assertNull(mockHttpServletResponse1.getRedirectedUrl());

		Assert.assertNull(httpSession.getAttribute(entitlementActivatedKey));

		MockHttpServletRequest mockHttpServletRequest2 =
			new MockHttpServletRequest();

		mockHttpServletRequest2.setRequestURI(
			PortalUtil.getPathContext() +
				"/c/portal/license_activation_success");
		mockHttpServletRequest2.setSession(httpSession);

		MockHttpServletResponse mockHttpServletResponse2 =
			new MockHttpServletResponse();

		lifecycleAction.processLifecycleEvent(
			new LifecycleEvent(
				mockHttpServletRequest2, mockHttpServletResponse2));

		Assert.assertEquals(
			PortalUtil.getPathContext() + "/c/portal/layout",
			mockHttpServletResponse2.getRedirectedUrl());
	}

	@Test
	public void testGetRedirectForLicenseActivationSuccessWithoutSessionFlag()
		throws Exception {

		MockHttpServletRequest mockHttpServletRequest =
			new MockHttpServletRequest();

		mockHttpServletRequest.setRequestURI(
			PortalUtil.getPathContext() +
				"/c/portal/license_activation_success");

		MockHttpServletResponse mockHttpServletResponse =
			new MockHttpServletResponse();

		lifecycleAction.processLifecycleEvent(
			new LifecycleEvent(
				mockHttpServletRequest, mockHttpServletResponse));

		Assert.assertEquals(
			PortalUtil.getPathContext() + "/c/portal/layout",
			mockHttpServletResponse.getRedirectedUrl());
	}

	@Test
	public void testGetRedirectForValidPortalActivationRequest()
		throws Exception {

		try (SafeCloseable safeCloseable1 = disableValidateWithSafeCloseable();
			SafeCloseable safeCloseable2 = resetLicenseDataWithSafeCloseble()) {

			deployEnterprisePortalLicense(
				RandomTestUtil.randomString(), "7",
				EntitlementManager.getInstallationId(), Time.HOUR);

			setEntitlementState(EntitlementConstants.STATE_GOOD);

			MockHttpServletRequest mockHttpServletRequest =
				new MockHttpServletRequest();

			mockHttpServletRequest.setRequestURI(
				PortalUtil.getPathContext() + "/c/portal/license_activation");

			MockHttpServletResponse mockHttpServletResponse =
				new MockHttpServletResponse();

			lifecycleAction.processLifecycleEvent(
				new LifecycleEvent(
					mockHttpServletRequest, mockHttpServletResponse));

			Assert.assertEquals(
				PortalUtil.getPathContext() + "/c/portal/layout",
				mockHttpServletResponse.getRedirectedUrl());
		}
	}

}
/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.license.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.portal.kernel.license.util.App;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Tina Tian
 */
@RunWith(Arquillian.class)
public class CheckLicenseTest extends BaseCheckLicenseTestCase {

	@Test
	public void testCheckLicenseWithFreeTierBinaryFile2026_Q1_0()
		throws Exception {

		testCheckLicense("free_tier_binary_file_2026_Q1_0.li");
	}

	@Test
	public void testCheckLicenseWithFreeTierBinaryFileAfter2026_Q1_0()
		throws Exception {

		testCheckLicense("free_tier_binary_file_after_2026_Q1_0.li");
	}

	@Override
	protected App[] getApps() {
		return App.values();
	}

}
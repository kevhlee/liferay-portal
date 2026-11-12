/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.license.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.portal.ee.license.EntitlementConstants;
import com.liferay.portal.ee.license.EntitlementManager;
import com.liferay.portal.ee.license.shared.Encryptor;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.util.PropertiesUtil;
import com.liferay.portal.kernel.util.Time;

import java.io.File;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Kevin Lee
 */
@RunWith(Arquillian.class)
public class StartupInitEntitlementTest extends BaseEntitlementTestCase {

	@Test
	public void testInit() throws Exception {
		setEntitlementState(EntitlementConstants.STATE_GOOD);

		EntitlementManager.saveEntitlementState();

		setEntitlementState(EntitlementConstants.STATE_NOT_ACTIVATED);

		EntitlementManager.init();

		Assert.assertEquals(
			EntitlementConstants.STATE_GOOD,
			EntitlementManager.getEntitlementState());
	}

	@Test
	public void testInitWithDifferentInstallationId() throws Exception {
		setEntitlementState(EntitlementConstants.STATE_GOOD);

		EntitlementManager.saveEntitlementState();

		File file = getEntitlementFile();

		long lastModifiedTime = file.lastModified();

		Properties properties = new Properties();

		properties.setProperty(
			"installation.id", RandomTestUtil.randomString());
		properties.setProperty(
			"last.modified.time", String.valueOf(lastModifiedTime));

		Files.write(
			file.toPath(),
			Encryptor.encrypt(
				PropertiesUtil.toString(
					properties
				).getBytes(
					StandardCharsets.UTF_8
				)));

		Assert.assertTrue(file.setLastModified(lastModifiedTime));

		setEntitlementState(EntitlementConstants.STATE_NOT_ACTIVATED);

		EntitlementManager.init();

		Assert.assertFalse(file.exists());

		Assert.assertEquals(
			EntitlementConstants.STATE_NOT_ACTIVATED,
			EntitlementManager.getEntitlementState());
	}

	@Test
	public void testInitWithDifferentLastModifiedTime() throws Exception {
		setEntitlementState(EntitlementConstants.STATE_GOOD);

		EntitlementManager.saveEntitlementState();

		setEntitlementState(EntitlementConstants.STATE_NOT_ACTIVATED);

		File file = getEntitlementFile();

		Assert.assertTrue(file.setLastModified(file.lastModified() - Time.DAY));

		EntitlementManager.init();

		Assert.assertFalse(file.exists());

		Assert.assertEquals(
			EntitlementConstants.STATE_NOT_ACTIVATED,
			EntitlementManager.getEntitlementState());
	}

	@Test
	public void testInitWithoutEntitlementFile() {
		EntitlementManager.init();

		Assert.assertEquals(
			EntitlementConstants.STATE_NOT_ACTIVATED,
			EntitlementManager.getEntitlementState());
	}

}
/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.cluster.multiple.internal.jgroups;

import com.liferay.portal.kernel.util.DigesterUtil;

import javax.crypto.spec.SecretKeySpec;

import org.jgroups.protocols.SYM_ENCRYPT;
import org.jgroups.stack.Protocol;
import org.jgroups.stack.ProtocolHook;

/**
 * @author Kevin Lee
 */
public class SymEncryptKeyProtocolHook implements ProtocolHook {

	public static void clearClusterName() {
		_clusterNames.remove();
	}

	public static void setClusterName(String clusterName) {
		_clusterNames.set(clusterName);
	}

	@Override
	public void afterCreation(Protocol protocol) {
		if (!(protocol instanceof SYM_ENCRYPT symEncrypt)) {
			return;
		}

		symEncrypt.setSecretKey(
			new SecretKeySpec(
				DigesterUtil.digestRaw(
					DigesterUtil.SHA_256, _clusterNames.get()),
				"AES"));
	}

	private static final ThreadLocal<String> _clusterNames =
		new ThreadLocal<>();

}
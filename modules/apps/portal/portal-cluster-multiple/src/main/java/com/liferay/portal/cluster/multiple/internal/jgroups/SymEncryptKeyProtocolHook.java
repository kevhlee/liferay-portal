/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.cluster.multiple.internal.jgroups;

import com.liferay.portal.kernel.util.DigesterUtil;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.Validator;

import javax.crypto.spec.SecretKeySpec;

import org.jgroups.protocols.SYM_ENCRYPT;
import org.jgroups.stack.Protocol;
import org.jgroups.stack.ProtocolHook;

/**
 * @author Kevin Lee
 */
public class SymEncryptKeyProtocolHook implements ProtocolHook {

	@Override
	public void afterCreation(Protocol protocol) {
		if (!(protocol instanceof SYM_ENCRYPT symEncrypt)) {
			return;
		}

		String clusterLinkAuthValue = PropsUtil.get(
			PropsKeys.CLUSTER_LINK_AUTH_VALUE);

		if (Validator.isNull(clusterLinkAuthValue)) {
			throw new IllegalStateException(
				"The portal property \"" + PropsKeys.CLUSTER_LINK_AUTH_VALUE +
					"\" must be set");
		}

		byte[] bytes = DigesterUtil.digestRaw(
			DigesterUtil.SHA_256, clusterLinkAuthValue);

		symEncrypt.setSecretKey(new SecretKeySpec(bytes, "AES"));
	}

}
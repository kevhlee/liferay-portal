/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.source.formatter.check;

import com.liferay.petra.string.CharPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.source.formatter.check.util.JavaSourceUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Tamyris Bernardo
 */
public class UpgradeGetImagePreviewURLMethodCheck
	extends BaseUpgradeMatcherReplacementCheck {

	@Override
	protected String afterFormat(
		String fileName, String absolutePath, String content,
		String newContent) {

		if (fileName.endsWith(".java")) {
			newContent = JavaSourceUtil.addImports(
				newContent, "com.liferay.document.library.util.DLURLHelper");
			newContent = StringUtil.replaceLast(
				newContent, CharPool.CLOSE_CURLY_BRACE,
				"\t@Reference\n\tprivate DLURLHelper _dlURLHelper;\n\n}");
		}

		return newContent;
	}

	@Override
	protected String format(
		String content, String newContent, Matcher matcher) {

		String methodCall = matcher.group();

		return StringUtil.replace(
			newContent, methodCall,
			StringUtil.replace(methodCall, "DLUtil", "_dlURLHelper"));
	}

	@Override
	protected Pattern getPattern() {
		return Pattern.compile("DLUtil\\.\\s*getImagePreviewURL\\(");
	}

	@Override
	protected boolean isValidExtension(String fileName) {
		if (!fileName.endsWith(".java") && !fileName.endsWith(".jsp")) {
			return false;
		}

		return true;
	}

}
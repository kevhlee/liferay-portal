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

import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.source.formatter.check.util.JavaSourceUtil;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Tamyris Bernardo
 */
public class UpgradeJavaUpdateLayoutParameterCheck extends BaseFileCheck {

	@Override
	protected String doProcess(
			String fileName, String absolutePath, String content)
		throws Exception {

		if (!fileName.endsWith(".java")) {
			return content;
		}

		String newContent = content;

		Matcher matcher = _updateLayoutPattern.matcher(content);

		while (matcher.find()) {
			String methodCall = JavaSourceUtil.getMethodCall(
				content, matcher.start());

			List<String> parameterList = JavaSourceUtil.getParameterList(
				methodCall);

			String[] parameterTypes = {
				"long", "boolean", "long", "long",
				"Map<java.util.Locale, String>",
				"Map<java.util.Locale, String>",
				"Map<java.util.Locale, String>",
				"Map<java.util.Locale, String>",
				"Map<java.util.Locale, String>", "String", "boolean",
				"Map<java.util.Locale, String>", "boolean", "byte[]",
				"ServiceContext"
			};

			if ((parameterList.size() != 15) ||
				!hasParameterTypes(content, parameterList, parameterTypes)) {

				continue;
			}

			String variableName = getVariableName(methodCall);

			if (hasClassOrVariableName(
					"LayoutLocalService", newContent, methodCall) ||
				variableName.contains("LayoutLocalServiceUtil")) {

				String newMethod = JavaSourceUtil.addMethodNewParameters(
					parameterList.size() - 1, methodCall, ".updateLayout(",
					Arrays.asList("0", "0", "0"), variableName);

				newContent = StringUtil.replace(
					newContent, methodCall, newMethod);
			}
		}

		return newContent;
	}

	private static final Pattern _updateLayoutPattern = Pattern.compile(
		"(\\t*?\\w+)\\.updateLayout\\(");

}
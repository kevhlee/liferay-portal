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

import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.petra.string.StringUtil;
import com.liferay.source.formatter.check.util.JavaSourceUtil;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Tamyris Bernardo
 */
public class UpgradeJavaSearchVocabulariesMethodCheck extends BaseFileCheck {

	@Override
	protected String doProcess(
			String fileName, String absolutePath, String content)
		throws Exception {

		if (!fileName.endsWith(".java")) {
			return content;
		}

		String newContent = content;

		boolean replaced = false;

		Matcher matcher = _searchVocabulariesPattern.matcher(content);

		while (matcher.find()) {
			String methodCall = JavaSourceUtil.getMethodCall(
				content, matcher.start());

			if (!hasClassOrVariableName(
					"AssetVocabularyService", content, methodCall) &&
				!hasClassOrVariableName(
					"AssetVocabularyLocalService", content, methodCall)) {

				continue;
			}

			List<String> parameterList = JavaSourceUtil.getParameterList(
				methodCall);

			String[] parameterTypes = {"long", "long", "String", "int", "int"};

			if ((parameterList.size() != 5) ||
				!hasParameterTypes(content, parameterList, parameterTypes)) {

				continue;
			}

			String newMethod = _addOrReplaceParameters(
				matcher.group(0), methodCall, parameterList);

			newContent = StringUtil.replace(newContent, methodCall, newMethod);

			replaced = true;
		}

		if (replaced) {
			newContent = JavaSourceUtil.addImports(
				newContent, "com.liferay.portal.kernel.search.Sort");
		}

		return newContent;
	}

	private String _addOrReplaceParameters(
		String group, String methodCall, List<String> parameterList) {

		parameterList.add(3, "new int[]{}");
		parameterList.add(6, "new Sort()");
		parameterList.set(
			1,
			StringBundler.concat(
				"new long[]{", parameterList.get(1),
				StringPool.CLOSE_CURLY_BRACE));

		StringBundler sb = new StringBundler(3);

		sb.append(group);
		sb.append(StringUtil.merge(parameterList, StringPool.COMMA_AND_SPACE));
		sb.append(StringPool.CLOSE_PARENTHESIS);

		return StringUtil.replace(methodCall, methodCall, sb.toString());
	}

	private static final Pattern _searchVocabulariesPattern = Pattern.compile(
		"\\w+\\.searchVocabularies\\(");

}
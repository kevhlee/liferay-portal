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

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Nícolas Moura
 */
public class UpgradeJavaAddFolderParameterCheck
	extends BaseUpgradeMatcherReplacementCheck {

	@Override
	protected String format(
		String content, String newContent, Matcher matcher) {

		String methodCall = JavaSourceUtil.getMethodCall(
			content, matcher.start());

		List<String> parameterList = JavaSourceUtil.getParameterList(
			methodCall);

		if (parameterList.size() == 7) {
			return newContent;
		}

		if (methodCall.contains("JournalFolderLocalServiceUtil")) {
			return  _addParameter(newContent, methodCall);
		}

		String variable = methodCall.substring(
			0, methodCall.indexOf(CharPool.PERIOD));

		Pattern variableDeclarationPattern = Pattern.compile(
			"[A-Za-z_]+ " + variable);

		Matcher variableDeclarationMatcher = variableDeclarationPattern.matcher(
			content);

		if (variableDeclarationMatcher.find()) {
			String variableDeclaration = variableDeclarationMatcher.group();

			if (variableDeclaration.contains("JournalFolderService") ||
				variableDeclaration.contains("JournalFolderLocalService")) {

				newContent = _addParameter(newContent, methodCall);
			}
		}

		return newContent;
	}

	@Override
	protected Pattern getPattern() {
		return Pattern.compile("\\w+\\.addFolder\\(");
	}

	private String _addParameter(String content, String methodCall) {
		return StringUtil.replace(
			content, methodCall,
			StringUtil.replace(methodCall, ".addFolder(", ".addFolder(null, "));
	}

}
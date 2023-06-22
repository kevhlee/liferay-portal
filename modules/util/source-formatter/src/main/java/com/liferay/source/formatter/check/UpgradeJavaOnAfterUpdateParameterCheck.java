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
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.source.formatter.check.util.JavaSourceUtil;
import com.liferay.source.formatter.parser.JavaClass;
import com.liferay.source.formatter.parser.JavaTerm;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Tamyris Torres
 */
public class UpgradeJavaOnAfterUpdateParameterCheck extends BaseJavaTermCheck {

	@Override
	protected String doProcess(
			String fileName, String absolutePath, JavaTerm javaTerm,
			String fileContent)
		throws Exception {

		JavaClass javaClass = (JavaClass)javaTerm;

		if (!_extendsFromBaseModelListener(javaClass)) {
			return fileContent;
		}

		String newContent = javaTerm.getContent();

		for (JavaTerm childJavaTerms : javaClass.getChildJavaTerms()) {
			String content = childJavaTerms.getContent();

			Matcher onAfterUpdateMatcher = _onAfterUpdatePattern.matcher(
				content);

			while (onAfterUpdateMatcher.find()) {
				newContent = _format(
					JavaSourceUtil.getMethodCall(
						content, onAfterUpdateMatcher.start()),
					newContent);
			}
		}

		return newContent;
	}

	@Override
	protected String[] getCheckableJavaTermNames() {
		return new String[] {JAVA_CLASS};
	}

	private String _cloneAndRenameFirstParameter(List<String> parameterList) {
		String firstParameter = parameterList.get(0);

		int firstParameterNameIndex = firstParameter.indexOf(" ") + 1;

		StringBundler parametersSB = new StringBundler(6);

		parametersSB.append(
			firstParameter.substring(0, firstParameterNameIndex));
		parametersSB.append("original");
		parametersSB.append(
			Character.toUpperCase(
				firstParameter.charAt(firstParameterNameIndex)));
		parametersSB.append(
			firstParameter.substring(firstParameterNameIndex + 1));
		parametersSB.append(StringPool.COMMA_AND_SPACE);
		parametersSB.append(parameterList.get(0));

		return parametersSB.toString();
	}

	private boolean _extendsFromBaseModelListener(JavaClass javaClass) {
		List<String> extendedClassNames = javaClass.getExtendedClassNames();

		if (extendedClassNames.contains("BaseModelListener")) {
			return true;
		}

		return false;
	}

	private String _format(String methodCall, String newContent) {
		List<String> parameterList = JavaSourceUtil.getParameterList(
			methodCall);

		if (parameterList.size() != 1) {
			return newContent;
		}

		String newParameters = _cloneAndRenameFirstParameter(parameterList);

		return StringUtil.replace(
			newContent, methodCall,
			StringUtil.replace(
				methodCall, "onAfterUpdate(" + parameterList.get(0),
				"onAfterUpdate(" + newParameters));
	}

	private static final Pattern _onAfterUpdatePattern = Pattern.compile(
		" void\\s*onAfterUpdate\\(");

}
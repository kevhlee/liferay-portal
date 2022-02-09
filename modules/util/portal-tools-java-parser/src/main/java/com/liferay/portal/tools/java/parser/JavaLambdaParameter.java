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

package com.liferay.portal.tools.java.parser;

import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;

import java.util.List;

/**
 * @author Hugo Huijser
 */
public class JavaLambdaParameter extends BaseJavaTerm {

	public JavaLambdaParameter(String name) {
		this(name, null, null);
	}

	public JavaLambdaParameter(
		String name, List<JavaAnnotation> javaAnnotations,
		List<JavaSimpleValue> modifiers) {

		_name = new JavaSimpleValue(name);
		_javaAnnotations = javaAnnotations;
		_modifiers = modifiers;
	}

	public boolean hasJavaType() {
		if (_javaType == null) {
			return false;
		}

		return true;
	}

	public void setJavaType(JavaType javaType) {
		_javaType = javaType;
	}

	@Override
	public String toString(
		String indent, String prefix, String suffix, int maxLineLength) {

		if (_javaType == null) {
			return _name.toString(indent, prefix, suffix, maxLineLength);
		}

		StringBundler sb = new StringBundler();

		for (int i = 0; i < _javaAnnotations.size(); i++) {
			if (i == 0) {
				appendNewLine(
					sb, _javaAnnotations.get(i), indent, prefix, " ",
					maxLineLength);

				prefix = StringPool.BLANK;
			}
			else {
				appendNewLine(
					sb, _javaAnnotations.get(i), indent, maxLineLength);
			}
		}

		sb.append(indent);

		indent = "\t" + indent;

		if (!_modifiers.isEmpty()) {
			indent = append(sb, _modifiers, indent, prefix, " ", maxLineLength);

			prefix = StringPool.BLANK;
		}

		indent = append(sb, _javaType, indent, prefix, " ", maxLineLength);

		append(sb, _name, indent, "", suffix, maxLineLength);

		return sb.toString();
	}

	private List<JavaAnnotation> _javaAnnotations;
	private JavaType _javaType;
	private List<JavaSimpleValue> _modifiers;
	private final JavaSimpleValue _name;

}
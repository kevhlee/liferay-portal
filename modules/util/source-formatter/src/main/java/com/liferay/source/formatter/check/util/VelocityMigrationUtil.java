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

package com.liferay.source.formatter.check.util;

import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.source.formatter.util.FileUtil;

import java.io.File;
import java.io.IOException;

/**
 * @author Nícolas Moura
 */
public class VelocityMigrationUtil {

	public static String getFTLMigratedContent(
			String content, String velocityFileName)
		throws IOException {

		File file = _getFTLMigratedFile(velocityFileName);

		if (file.length() != 0) {
			content = FileUtil.read(file);
		}

		return content;
	}

	public static void writeMigratedContent(
			String migratedContent, String velocityFileName)
		throws IOException {

		FileUtil.write(_getFTLMigratedFile(velocityFileName), migratedContent);
	}

	private static File _getFTLMigratedFile(String fileName) {
		int periodIndex = fileName.lastIndexOf(StringPool.PERIOD);
		int slashIndex = fileName.lastIndexOf(StringPool.SLASH);

		return new File(
			StringBundler.concat(
				fileName.substring(0, slashIndex), "/migrated/",
				fileName.substring(slashIndex + 1, periodIndex), ".ftl"));
	}

}
/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.test.log;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Kevin Lee
 */
public class LogCaptureTest {

	@Test
	public void testResetPriority() {
		LogTester logTester = new LogTester();

		try (LogCapture logCapture = LoggerTestUtil.configureLog4JLogger(
				LogTester.class.getName(), LoggerTestUtil.DEBUG)) {

			logTester.run();

			List<LogEntry> logEntries = logCapture.getLogEntries();

			Assert.assertTrue(logEntries.toString(), logEntries.isEmpty());

			logCapture.resetPriority(LoggerTestUtil.DEBUG);

			logTester.run();

			logEntries = logCapture.getLogEntries();

			Assert.assertFalse(logEntries.toString(), logEntries.isEmpty());
		}

		try (LogCapture logCapture = LoggerTestUtil.configureLog4JLogger(
				LogTester.class.getName(), LoggerTestUtil.DEBUG)) {

			logTester.run();

			List<LogEntry> logEntries = logCapture.getLogEntries();

			Assert.assertTrue(logEntries.toString(), logEntries.isEmpty());
		}
	}

	private static class LogTester {

		public void run() {
			if (_log.isDebugEnabled()) {
				_log.debug("Hello, world!");
			}
		}

		private static final Log _log = LogFactoryUtil.getLog(LogTester.class);

	}

}
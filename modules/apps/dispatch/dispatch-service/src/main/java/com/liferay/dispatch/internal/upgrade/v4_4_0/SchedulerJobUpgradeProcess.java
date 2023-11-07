/**
 * SPDX-FileCopyrightText: (c) 2023 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.dispatch.internal.upgrade.v4_4_0;

import com.liferay.dispatch.constants.DispatchConstants;
import com.liferay.dispatch.executor.DispatchTaskClusterMode;
import com.liferay.portal.kernel.scheduler.SchedulerEngineHelper;
import com.liferay.portal.kernel.scheduler.StorageType;
import com.liferay.portal.kernel.scheduler.messaging.SchedulerResponse;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.util.GetterUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Kevin Lee
 */
public class SchedulerJobUpgradeProcess extends UpgradeProcess {

	public SchedulerJobUpgradeProcess(
		SchedulerEngineHelper schedulerEngineHelper) {

		_schedulerEngineHelper = schedulerEngineHelper;
	}

	@Override
	protected void doUpgrade() throws Exception {
		Set<Long> dispatchTriggerIds = new HashSet<>();

		try (PreparedStatement preparedStatement = connection.prepareStatement(
				"select dispatchTriggerId from DispatchTrigger where " +
					"dispatchTaskClusterMode in (?, ?)")) {

			preparedStatement.setInt(
				1, DispatchTaskClusterMode.NOT_APPLICABLE.getMode());
			preparedStatement.setInt(
				2, DispatchTaskClusterMode.SINGLE_NODE_PERSISTED.getMode());

			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				while (resultSet.next()) {
					dispatchTriggerIds.add(
						resultSet.getLong("dispatchTriggerId"));
				}
			}
		}

		if (dispatchTriggerIds.isEmpty()) {
			return;
		}

		for (SchedulerResponse schedulerResponse :
				_schedulerEngineHelper.getScheduledJobs(
					StorageType.PERSISTED)) {

			if (!Objects.equals(
					schedulerResponse.getDestinationName(),
					DispatchConstants.EXECUTOR_DESTINATION_NAME)) {

				continue;
			}

			Matcher matcher = _pattern.matcher(schedulerResponse.getJobName());

			long dispatchTriggerId = GetterUtil.getLong(matcher.group(1));

			if (!dispatchTriggerIds.contains(dispatchTriggerId)) {
				continue;
			}

			_schedulerEngineHelper.delete(
				schedulerResponse.getJobName(),
				schedulerResponse.getGroupName(), StorageType.PERSISTED);

			_schedulerEngineHelper.schedule(
				schedulerResponse.getTrigger(), StorageType.PERSISTED,
				schedulerResponse.getDescription(),
				schedulerResponse.getDestinationName(),
				schedulerResponse.getMessage());
		}
	}

	private static final Pattern _pattern = Pattern.compile(
		"DISPATCH_JOB_(\\d+)");

	private final SchedulerEngineHelper _schedulerEngineHelper;

}
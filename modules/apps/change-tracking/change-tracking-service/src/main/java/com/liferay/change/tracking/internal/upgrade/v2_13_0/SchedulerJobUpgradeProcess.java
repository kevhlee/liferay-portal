/**
 * SPDX-FileCopyrightText: (c) 2023 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.change.tracking.internal.upgrade.v2_13_0;

import com.liferay.change.tracking.constants.CTDestinationNames;
import com.liferay.portal.kernel.messaging.Message;
import com.liferay.portal.kernel.scheduler.SchedulerEngineHelper;
import com.liferay.portal.kernel.scheduler.StorageType;
import com.liferay.portal.kernel.scheduler.messaging.SchedulerResponse;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.workflow.WorkflowConstants;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.HashSet;
import java.util.Set;

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
		Set<Long> ctCollectionIds = new HashSet<>();

		try (PreparedStatement preparedStatement = connection.prepareStatement(
				"select ctCollectionId from CTCollection where status = " +
					WorkflowConstants.STATUS_SCHEDULED);
			ResultSet resultSet = preparedStatement.executeQuery()) {

			while (resultSet.next()) {
				ctCollectionIds.add(resultSet.getLong("ctCollectionId"));
			}
		}

		if (ctCollectionIds.isEmpty()) {
			return;
		}

		for (SchedulerResponse schedulerResponse :
				_schedulerEngineHelper.getScheduledJobs(
					CTDestinationNames.CT_COLLECTION_SCHEDULED_PUBLISH,
					StorageType.PERSISTED)) {

			Message message = schedulerResponse.getMessage();

			if (!ctCollectionIds.contains(message.getLong("ctCollectionId"))) {
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

	private final SchedulerEngineHelper _schedulerEngineHelper;

}
/**
 * SPDX-FileCopyrightText: (c) 2023 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.exportimport.internal.upgrade.v1_1_0;

import com.liferay.exportimport.kernel.model.ExportImportConfiguration;
import com.liferay.exportimport.kernel.service.ExportImportConfigurationLocalService;
import com.liferay.portal.kernel.messaging.DestinationNames;
import com.liferay.portal.kernel.messaging.Message;
import com.liferay.portal.kernel.scheduler.SchedulerEngineHelper;
import com.liferay.portal.kernel.scheduler.StorageType;
import com.liferay.portal.kernel.scheduler.messaging.SchedulerResponse;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.util.GetterUtil;

/**
 * @author Kevin Lee
 */
public class SchedulerJobUpgradeProcess extends UpgradeProcess {

	public SchedulerJobUpgradeProcess(
		ExportImportConfigurationLocalService
			exportImportConfigurationLocalService,
		SchedulerEngineHelper schedulerEngineHelper) {

		_exportImportConfigurationLocalService =
			exportImportConfigurationLocalService;
		_schedulerEngineHelper = schedulerEngineHelper;
	}

	@Override
	protected void doUpgrade() throws Exception {
		for (SchedulerResponse schedulerResponse :
				_schedulerEngineHelper.getScheduledJobs(
					StorageType.PERSISTED)) {

			String destinationName = schedulerResponse.getDestinationName();

			if (!(destinationName.equals(
					DestinationNames.LAYOUTS_LOCAL_PUBLISHER) ||
				  destinationName.equals(
					  DestinationNames.LAYOUTS_REMOTE_PUBLISHER))) {

				continue;
			}

			Message message = schedulerResponse.getMessage();

			long exportImportConfigurationId = GetterUtil.getLong(
				message.getPayload());

			ExportImportConfiguration exportImportConfiguration =
				_exportImportConfigurationLocalService.
					fetchExportImportConfiguration(exportImportConfigurationId);

			if (exportImportConfiguration == null) {
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

	private final ExportImportConfigurationLocalService
		_exportImportConfigurationLocalService;
	private final SchedulerEngineHelper _schedulerEngineHelper;

}
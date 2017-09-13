package com.spectralogic.dsbrowser.gui.services.tasks.transfer;/*
 * ****************************************************************************
 *    Copyright 2014-2017 Spectra Logic Corporation. All Rights Reserved.
 *    Licensed under the Apache License, Version 2.0 (the "License"). You may not use
 *    this file except in compliance with the License. A copy of the License is located at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *    or in the "license" file accompanying this file.
 *    This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *    CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *    specific language governing permissions and limitations under the License.
 *  ****************************************************************************
 */

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.helpers.Ds3ClientHelpers;
import com.spectralogic.ds3client.metadata.MetadataReceivedListenerImpl;
import com.spectralogic.ds3client.models.Contents;
import com.spectralogic.ds3client.models.Priority;
import com.spectralogic.ds3client.models.bulk.Ds3Object;
import com.spectralogic.ds3client.networking.Metadata;
import com.spectralogic.dsbrowser.api.services.logging.LogType;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValueCustom;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.util.DateFormat;
import com.spectralogic.dsbrowser.gui.util.PathUtil;
import com.spectralogic.dsbrowser.gui.util.StringBuilderUtil;
import com.spectralogic.dsbrowser.gui.util.StringConstants;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class DS3GetJob extends TransferJob {
    private static final Logger LOG = LoggerFactory.getLogger(DS3GetJob.class);

    DS3GetJob(
            final List<Ds3TreeTableValueCustom> selectedItems,
            final Path fileTreePath,
            final Ds3Client client,
            final String jobPriority,
            final int maximumNumberOfParallelThreads,
            final JobInterruptionStore jobInterruptionStore,
            final DeepStorageBrowserPresenter deepStorageBrowserPresenter,
            final Session currentSession,
            final ResourceBundle resourceBundle,
            final LoggingService loggingService) {
        super(
                client,
                resourceBundle,
                deepStorageBrowserPresenter,
                maximumNumberOfParallelThreads,
                currentSession.getEndpoint(),
                loggingService,
                () -> {
                    final String bucketName = selectedItems.get(0).getBucketName();
                    try {
                        return Ds3ClientHelpers.wrap(client, 100).startReadJob(bucketName, getDS3Objects(bucketName, selectedItems, client, loggingService)).withMaxParallelRequests(maximumNumberOfParallelThreads);
                    } catch (final IOException e) {
                        e.printStackTrace();
                    }
                    return null;
                },
                () -> fileTreePath.toString(),
                () -> getPriority(jobPriority),
                () -> getFileMap(selectedItems),
                () -> getFolderMap(selectedItems),
                (task, totalSent, totalJobSize) -> (final long l) -> {
                    task.updateProgress(totalSent.getAndAdd(l) / 2, totalJobSize);
                    totalSent.addAndGet(l);
                },
                task -> (final int i) -> {
                    try {
                        loggingService.logMessage("Attempting Retry", LogType.INFO);
                        Thread.sleep(1000 * i);
                    } catch (final InterruptedException e) {
                        LOG.error("Did not receive chunks before timeout", e);
                        loggingService.logMessage("Did not receive chunks before timeout", LogType.ERROR);
                    }
                },
                (task, startTime, totalSent, totalJobSize) -> (final String o) -> {
                    task.getTransferRates(startTime, totalSent, totalJobSize, o, fileTreePath.toString());
                    loggingService.logMessage(StringBuilderUtil.objectSuccessfullyTransferredString(o, fileTreePath.toString(), DateFormat.formatDate(new Date()), null).toString(), LogType.SUCCESS);
                },
                task -> {
                    final ImmutableMap.Builder<String,Path> fileMapperBuilder = ImmutableMap.builder();
                    fileMapperBuilder.putAll(getFileMap(selectedItems)).putAll(getFolderMap(selectedItems));
                    return (final String file) -> FileChannel.open(PathUtil.resolveForSymbolic(fileMapperBuilder.build().get(file)), StandardOpenOption.READ);
                },
                task -> null,
                task -> (final String string, final Metadata metadata) -> {
                    LOG.info("Restoring metadata for {}", string);
                    try {
                        new MetadataReceivedListenerImpl(fileTreePath.toString()).metadataReceived(string, metadata);
                    } catch (final Exception e) {
                        LOG.error("Error in metadata receiving", e);
                    }
                },
                Optional::empty,
                task -> null,
                jobInterruptionStore
        );
    }

    private static Optional<Priority> getPriority(final String jobPriority) {
        if (jobPriority == null) {
            return Optional.empty();
        } else {
            return Optional.of(Priority.valueOf(jobPriority));
        }
    }

    public static ImmutableMap<String, Path> getFileMap(final List<Ds3TreeTableValueCustom> selectedItems) {
        final ImmutableList<Ds3TreeTableValueCustom> fileList = selectedItems.stream().filter(value ->
                value.getType().equals(Ds3TreeTableValue.Type.File)).collect(GuavaCollectors.immutableList());
        final ImmutableMap.Builder<String, Path> fileMap = ImmutableMap.builder();
        fileList.forEach(file -> {
            fileMap.put(file.getFullName(), Paths.get(StringConstants.FORWARD_SLASH));
        });
        return fileMap.build();
    }

    public static ImmutableMap<String, Path> getFolderMap(final List<Ds3TreeTableValueCustom> selectedItems) {
        final ImmutableList<Ds3TreeTableValueCustom> folderList = selectedItems.stream().filter(value ->
                !value.getType().equals(Ds3TreeTableValue.Type.File)).collect(GuavaCollectors.immutableList());
        final ImmutableMap.Builder<String, Path> fileMap = ImmutableMap.builder();
        folderList.forEach(folder -> {
            fileMap.put(folder.getFullName(), Paths.get(StringConstants.FORWARD_SLASH));
        });
        return fileMap.build();
    }

    public static FluentIterable<Ds3Object> getDS3Objects(final String bucketName, final List<Ds3TreeTableValueCustom> selectedItems, final Ds3Client client, final LoggingService loggingService) {
        final FluentIterable<Contents> c = FluentIterable.from(new Contents[0]);
        selectedItems.forEach(selectedItem -> {
            try {
                c.append(Ds3ClientHelpers.wrap(client, 100).listObjects(bucketName, selectedItem.getFullName()));
            } catch (final IOException e) {
                LOG.error("Failed to list objects", e);
                loggingService.logMessage("Failed to list objects for " + bucketName, LogType.ERROR);
            }
        });
        return c.transform(contents -> {
            if (contents != null) {
                return new Ds3Object(contents.getKey(), contents.getSize());
            } else {
                return null;
            }
        });
    }
}

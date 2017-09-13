/*
*****************************************************************************
*Copyright 2014-2017Spectra Logic Corporation.All Rights Reserved.
*Licensed under the Apache License,Version2.0(the"License").You may not use
*this file except in compliance with the License.A copy of the License is located at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*or in the"license"file accompanying this file.
*This file is distributed on an"AS IS"BASIS,WITHOUT WARRANTIES OR
*CONDITIONS OF ANY KIND,either express or implied.See the License for the
*specific language governing permissions and limitations under the License.
*****************************************************************************
*/

package com.spectralogic.dsbrowser.gui.services.tasks.transfer;

import com.google.common.collect.ImmutableMap;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.commands.spectrads3.GetJobSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.ModifyJobSpectraS3Request;
import com.spectralogic.ds3client.helpers.*;
import com.spectralogic.ds3client.models.MasterObjectList;
import com.spectralogic.ds3client.models.Priority;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.tasks.Ds3JobTask;
import com.spectralogic.dsbrowser.gui.util.CheckNetwork;
import com.spectralogic.dsbrowser.gui.util.ParseJobInterruptionMap;
import javafx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.UUID;

public class TransferJob extends Ds3JobTask {

    private final static Logger LOG = LoggerFactory.getLogger(TransferJob.class);
    private final int maximumNumberOfParallelThreads;
    private final ImmutableMap<String, Path> fileMap;
    private final ImmutableMap<String, Path> folderMap;
    private final Optional<MetadataAccess> metadata;
    private final LocationBuilder locationBuilder;
    private final Optional<Priority> jobPriority;
    private final DataTransferredListener dataTransferredListener;
    private final WaitingForChunksListener waitingForChunksListener;
    private final ObjectCompletedListener objectCompletedListener;
    private final FailureEventListener failureEventListener;
    private final Ds3ClientHelpers.ObjectChannelBuilder objectChannelBuilder;
    private final ChecksumListener checksumListener;
    private final String endpoint;

    @Inject
    public TransferJob(final Ds3Client ds3Client,
            final ResourceBundle resourceBundle,
            final DeepStorageBrowserPresenter deepStorageBrowserPresenter,
            @Named("maximumNumberOfParallelThreads") final int maximumNumberOfParallelThreads,
            final String endpoint,
            final LoggingService loggingService,
            final JobBuilder jobBuilder,
            final MapBuilder mapBuilder,
            final LocationBuilder locationBuilder,
            final JobPriorityBuilder jobPriorityBuilder,
            final DataTransferredListener dataTransferredListener,
            final WaitingForChunksListener waitingForChunksListener,
            final ObjectCompletedListener objectCompletedListener,
            final Ds3ClientHelpers.ObjectChannelBuilder objectChannelBuilder,
            final ChecksumListener checksumListener,
            final MetadataBuilder metadataBuilder,
            final FailureEventListener failureEventListner) {
        this.maximumNumberOfParallelThreads = maximumNumberOfParallelThreads;
        this.ds3Client = ds3Client;
        this.resourceBundle = resourceBundle;
        this.locationBuilder = locationBuilder;
        this.deepStorageBrowserPresenter = deepStorageBrowserPresenter;
        this.endpoint = endpoint;
        this.loggingService = loggingService;
        final Pair<ImmutableMap<String, Path>, ImmutableMap<String, Path>> fileandFolder = mapBuilder.build();
        this.fileMap = fileandFolder.getKey();
        this.folderMap = fileandFolder.getValue();
        this.metadata = metadataBuilder.build();
        this.job = jobBuilder.build();
        this.jobPriority = jobPriorityBuilder.build();
        this.dataTransferredListener = dataTransferredListener;
        this.waitingForChunksListener = waitingForChunksListener;
        this.objectCompletedListener = objectCompletedListener;
        this.failureEventListener = failureEventListner;
        this.checksumListener = checksumListener;
        this.objectChannelBuilder = objectChannelBuilder;

    }

    @Override
    public void executeJob() throws Exception {
        if (!CheckNetwork.isReachable(ds3Client)) {
            hostNotAvailable();
            return;
        }
        if(job == null) {
            return;
        }
        job.withMaxParallelRequests(maximumNumberOfParallelThreads);
        metadata.ifPresent(metadataAccess -> job.withMetadata(metadataAccess));
        jobPriority.ifPresent(this::modifyPriority);
        final MasterObjectList mol = ds3Client.getJobSpectraS3(new GetJobSpectraS3Request(getJobId())).getMasterObjectListResult();
        final long totalSize = mol.getOriginalSizeInBytes();
        final String jobType = mol.getRequestType().toString();
        final String bukcet = mol.getBucketName();

        ParseJobInterruptionMap.saveValuesToFiles(JobInterruptionStore.loadJobIds(), fileMap, folderMap, endpoint, getJobId(), totalSize, locationBuilder.build(), jobType, bukcet);
        job.attachDataTransferredListener(dataTransferredListener);
        job.attachWaitingForChunksListener(waitingForChunksListener);
        job.attachObjectCompletedListener(objectCompletedListener);
        job.attachFailureEventListener(failureEventListener);
        job.attachChecksumListener(checksumListener);
        job.transfer(objectChannelBuilder);
        ParseJobInterruptionMap.removeJobIdFromFile(JobInterruptionStore.loadJobIds(), getJobId().toString(), endpoint);

    }

    private void modifyPriority(final Priority jobString) {
        try {
            ds3Client.modifyJobSpectraS3(new ModifyJobSpectraS3Request(job.getJobId()).withPriority(jobString));
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public UUID getJobId() {
        return job.getJobId();
    }
}

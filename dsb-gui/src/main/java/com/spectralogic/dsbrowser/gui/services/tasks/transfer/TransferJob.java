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

import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.commands.spectrads3.GetJobSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.ModifyJobSpectraS3Request;
import com.spectralogic.ds3client.helpers.MetadataAccess;
import com.spectralogic.ds3client.models.MasterObjectList;
import com.spectralogic.ds3client.models.Priority;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.tasks.Ds3JobTask;
import com.spectralogic.dsbrowser.gui.util.CheckNetwork;
import com.spectralogic.dsbrowser.gui.util.ParseJobInterruptionMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class TransferJob extends Ds3JobTask {

    private final static Logger LOG = LoggerFactory.getLogger(TransferJob.class);
    private final int maximumNumberOfParallelThreads;
    private final FileMapBuilder fileMapBuilder;
    private final FolderMapBuilder folderMapBuilder;
    private final MetadataBuilder metadataBuilder;
    private final JobPriorityBuilder jobPriorityBuilder;
    private final JobBuilder jobBuilder;
    private final LocationBuilder locationBuilder;
    private final DataTransferredListenerBuilder dataTransferredListenerBuilder;
    private final WaitingForChunksListenerBuilder waitingForChunksListenerBuilder;
    private final ObjectCompletedListenerBuider objectCompletedListenerBuilder;
    private final FailureEventListenerBuilder failureEventListenerBuilder;
    private final ObjectChannelBuilderBuilder objectChannelBuilderBuilder;
    private final ChecksumListenerBuilder checksumListenerBuilder;
    private final String endpoint;
    private final MetadataReceivedListenerBuilder metadataReceivedListenerBuilder;
    private final JobInterruptionStore jobInterruptionStore;

    @Inject
    public TransferJob(final Ds3Client ds3Client,
            final ResourceBundle resourceBundle,
            final DeepStorageBrowserPresenter deepStorageBrowserPresenter,
            @Named("maximumNumberOfParallelThreads") final int maximumNumberOfParallelThreads,
            final String endpoint,
            final LoggingService loggingService,
            final JobBuilder jobBuilder,
            final LocationBuilder locationBuilder,
            final JobPriorityBuilder jobPriorityBuilder,
            final FileMapBuilder fileMapBuilder,
            final FolderMapBuilder folderMapBuilder,
            final DataTransferredListenerBuilder dataTransferredListenerBuilder,
            final WaitingForChunksListenerBuilder waitingForChunksListenerBuilder,
            final ObjectCompletedListenerBuider objectCompletedListenerBuilder,
            final ObjectChannelBuilderBuilder objectChannelBuilderBuilder,
            final ChecksumListenerBuilder checksumListenerBuilder,
            final MetadataReceivedListenerBuilder metadataReceivedListenerBuilder,
            final MetadataBuilder metadataBuilder,
            final FailureEventListenerBuilder failureEventListnerBuilder,
            final JobInterruptionStore jobInterruptionStore) {
        this.fileMapBuilder = fileMapBuilder;
        this.folderMapBuilder = folderMapBuilder;
        this.maximumNumberOfParallelThreads = maximumNumberOfParallelThreads;
        this.ds3Client = ds3Client;
        this.resourceBundle = resourceBundle;
        this.locationBuilder = locationBuilder;
        this.deepStorageBrowserPresenter = deepStorageBrowserPresenter;
        this.endpoint = endpoint;
        this.loggingService = loggingService;
        this.jobBuilder = jobBuilder;
        this.jobPriorityBuilder = jobPriorityBuilder;
        this.dataTransferredListenerBuilder = dataTransferredListenerBuilder;
        this.waitingForChunksListenerBuilder = waitingForChunksListenerBuilder;
        this.objectCompletedListenerBuilder = objectCompletedListenerBuilder;
        this.failureEventListenerBuilder = failureEventListnerBuilder;
        this.checksumListenerBuilder = checksumListenerBuilder;
        this.objectChannelBuilderBuilder = objectChannelBuilderBuilder;
        this.metadataBuilder = metadataBuilder;
        this.metadataReceivedListenerBuilder = metadataReceivedListenerBuilder;
        this.jobInterruptionStore = jobInterruptionStore;

    }

    @Override
    public void executeJob() throws Exception {
        if (!CheckNetwork.isReachable(ds3Client)) {
            hostNotAvailable();
            return;
        }

        job = jobBuilder.build();
        final Optional<MetadataAccess> metadata = metadataBuilder.build();
        final Optional<Priority> jobPriority = jobPriorityBuilder.build();

        job.withMaxParallelRequests(maximumNumberOfParallelThreads);
        metadata.ifPresent(metadataAccess -> job.withMetadata(metadataAccess));
        jobPriority.ifPresent(this::modifyPriority);
        final AtomicLong totalSent = new AtomicLong(0L);
        final MasterObjectList mol = ds3Client.getJobSpectraS3(new GetJobSpectraS3Request(getJobId())).getMasterObjectListResult();
        final long totalSize = mol.getOriginalSizeInBytes();
        final String jobType = mol.getRequestType().toString();
        final String bukcet = mol.getBucketName();

        ParseJobInterruptionMap.saveValuesToFiles(jobInterruptionStore, fileMapBuilder.build(), folderMapBuilder.build(), endpoint, getJobId(), totalSize, locationBuilder.build(), jobType, bukcet);
        job.attachDataTransferredListener(dataTransferredListenerBuilder.build(this, totalSent, totalSize));
        job.attachWaitingForChunksListener(waitingForChunksListenerBuilder.build(this));
        job.attachObjectCompletedListener(objectCompletedListenerBuilder.build(this, Instant.now(), totalSent, totalSize));
        job.attachFailureEventListener(failureEventListenerBuilder.build(this));
        job.attachChecksumListener(checksumListenerBuilder.build(this));
        job.attachMetadataReceivedListener(metadataReceivedListenerBuilder.build(this));
        job.transfer(objectChannelBuilderBuilder.build(this));
        ParseJobInterruptionMap.removeJobIdFromFile(jobInterruptionStore, getJobId().toString(), endpoint);
    }

    private void modifyPriority(final Priority jobString) {
        try {
            ds3Client.modifyJobSpectraS3(new ModifyJobSpectraS3Request(job.getJobId()).withPriority(jobString));
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void updateProgress(final double workDone, final double max) {
        super.updateProgress(workDone, max);
    }

    @Override
    protected void getTransferRates(final Instant jobStartInstant, final AtomicLong totalSent, final long totalJobSize, final String sourceLocation, final String targetLocation) {
        super.getTransferRates(jobStartInstant, totalSent, totalJobSize, sourceLocation, targetLocation);
    }

    @Override
    public UUID getJobId() {
        return job.getJobId();
    }
}

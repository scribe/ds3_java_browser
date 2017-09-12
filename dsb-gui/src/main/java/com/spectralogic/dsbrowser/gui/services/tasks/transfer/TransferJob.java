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
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.tasks.Ds3JobTask;
import javafx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.ResourceBundle;
import java.util.UUID;

public class TransferJob extends Ds3JobTask{

    private final static Logger LOG = LoggerFactory.getLogger(TransferJob.class);
    private final JobBuilder jobBuilder;

    @Inject
    public TransferJob(final Ds3Client ds3Client,
            final ResourceBundle resourceBundle,
            final DeepStorageBrowserPresenter deepStorageBrowserPresenter,
            final Session currentSession,
            final LoggingService loggingService,
            final JobBuilder jobBuilder,
            final MapBuilder mapBuilder) {
        this.ds3Client = ds3Client;
        this.resourceBundle = resourceBundle;
        this.deepStorageBrowserPresenter = deepStorageBrowserPresenter;
        this.currentSession = currentSession;
        this.loggingService = loggingService;
        final Pair<ImmutableMap<String,Path>,ImmutableMap<String,Path>> fileandFolder = mapBuilder.build();
        final ImmutableMap<String,Path> fileMap = fileandFolder.getKey();
        final ImmutableMap<String,Path> folderMap = fileandFolder.getValue();
        final ImmutableMap.Builder<String,Path> allBuilder = ImmutableMap.builder();
        allBuilder.putAll(fileMap);
        allBuilder.putAll(folderMap);
        final ImmutableMap<String,Path> allMap = allBuilder.build();
        this.jobBuilder = jobBuilder;
    }

    @Override
    public void executeJob() throws Exception {
        this.job = jobBuilder.build();

    }

    @Override
    public UUID getJobId() {
        return null;
    }
}

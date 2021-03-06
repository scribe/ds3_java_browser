/*
 * ******************************************************************************
 *    Copyright 2016-2017 Spectra Logic Corporation. All Rights Reserved.
 *    Licensed under the Apache License, Version 2.0 (the "License"). You may not use
 *    this file except in compliance with the License. A copy of the License is located at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *    or in the "license" file accompanying this file.
 *    This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *    CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *    specific language governing permissions and limitations under the License.
 * ******************************************************************************
 */

package com.spectralogic.dsbrowser.integration.tasks;

import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.Ds3ClientBuilder;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.interruptedjobwindow.EndpointInfo;
import com.spectralogic.dsbrowser.gui.services.BuildInfoServiceImpl;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.FilesAndFolderMap;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.newSessionService.SessionModelService;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedCredentials;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSession;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.settings.SettingsStore;
import com.spectralogic.dsbrowser.gui.services.tasks.CreateConnectionTask;
import com.spectralogic.dsbrowser.gui.services.tasks.Ds3JobTask;
import com.spectralogic.dsbrowser.gui.services.tasks.RecoverInterruptedJob;
import com.spectralogic.dsbrowser.gui.util.ConfigProperties;
import com.spectralogic.dsbrowser.gui.util.DateTimeUtils;
import com.spectralogic.dsbrowser.gui.util.StringConstants;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.shape.Circle;
import org.controlsfx.control.TaskProgressView;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CountDownLatch;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeThat;

/**
 * THERE MUST BE AN INTERRUPTED JOB IN LOCAL FILE SYSTEM TO SUCCESSFULLY RUN THIS TEST CASE
 */
public class RecoverInterruptedJobTest {

    private final static DateTimeUtils DTU = new DateTimeUtils(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    private final static Logger LOG = LoggerFactory.getLogger(RecoverInterruptedJobTest.class);
    private static final JobWorkers jobWorkers = new JobWorkers(10);
    private static Session session;
    private static RecoverInterruptedJob recoverInterruptedJob;
    private static final String fileName = "SampleFiles.txt";
    private boolean successFlag = false;
    private final static ResourceBundle resourceBundle = ResourceBundle.getBundle("lang", new Locale(ConfigProperties.getInstance().getLanguage()));
    private static final Ds3Client client = Ds3ClientBuilder.fromEnv().withHttps(false).build();
    private static final BuildInfoServiceImpl buildInfoService = new BuildInfoServiceImpl();

    @BeforeClass
    public static void setConnection() {
        new JFXPanel();
        Platform.runLater(() -> {
            try {
                final SavedSession savedSession = new SavedSession(
                        "CreateBucketTaskTest",
                        client.getConnectionDetails().getEndpoint(),
                        "80",
                        null,
                        new SavedCredentials(
                                client.getConnectionDetails().getCredentials().getClientId(),
                                client.getConnectionDetails().getCredentials().getKey()),
                        false,
                        false);
                session = new CreateConnectionTask().createConnection(SessionModelService.setSessionModel(savedSession, false), resourceBundle, buildInfoService);
                final Ds3Client ds3Client = session.getClient();
                final DeepStorageBrowserPresenter deepStorageBrowserPresenter = Mockito.mock(DeepStorageBrowserPresenter.class);

                Mockito.when(deepStorageBrowserPresenter.getNumInterruptedJobsCircle()).thenReturn(Mockito.mock(Circle.class));
                Mockito.when(deepStorageBrowserPresenter.getNumInterruptedJobsLabel()).thenReturn(Mockito.mock(Label.class));
                Mockito.when(deepStorageBrowserPresenter.getRecoverInterruptedJobsButton()).thenReturn(Mockito.mock(Button.class));
                final Ds3Common ds3Common = Mockito.mock(Ds3Common.class);
                Mockito.when(ds3Common.getCurrentSession()).thenReturn(session);
                Mockito.when(ds3Common.getDeepStorageBrowserPresenter()).thenReturn(deepStorageBrowserPresenter);

                final JobInterruptionStore jobInterruptionStore = JobInterruptionStore.loadJobIds();
                final Optional<Map<String, Map<String, FilesAndFolderMap>>> endPointsMapElement = jobInterruptionStore.getJobIdsModel()
                        .getEndpoints().stream().filter(endpoint -> endpoint.containsKey(session.getEndpoint() + StringConstants
                                .COLON + session.getPortNo())).findFirst();
                if (endPointsMapElement.isPresent()) {
                    final Map<String, Map<String, FilesAndFolderMap>> endPointsMap = endPointsMapElement.get();
                    final Map<String, FilesAndFolderMap> stringFilesAndFolderMapMap = endPointsMap.get(session.getEndpoint() + StringConstants.COLON + session.getPortNo());
                    final Optional<String> jobIdKeyElement = stringFilesAndFolderMapMap.entrySet().stream()
                            .map(Map.Entry::getKey)
                            .findFirst();
                    final EndpointInfo endPointInfo = new EndpointInfo(session.getEndpoint() + StringConstants.COLON + session.getPortNo(),
                            ds3Client,
                            stringFilesAndFolderMapMap,
                            deepStorageBrowserPresenter,
                            ds3Common
                    );
                    if(jobIdKeyElement.isPresent()) {
                        final String jobIdKey = jobIdKeyElement.get();
                        final TaskProgressView<Ds3JobTask> taskProgressView = new TaskProgressView<>();
                        Mockito.when(deepStorageBrowserPresenter.getJobProgressView()).thenReturn(taskProgressView);
                        Mockito.when(ds3Common.getDeepStorageBrowserPresenter().getJobProgressView()).thenReturn(taskProgressView);
                        recoverInterruptedJob = new RecoverInterruptedJob(UUID.fromString(jobIdKey), endPointInfo, jobInterruptionStore, client, null, SettingsStore.createDefaultSettingStore(), DTU, resourceBundle);
                        taskProgressView.getTasks().add(recoverInterruptedJob);
                    }
                    else {
                        LOG.info("No job available to recover");
                    }
                } else {
                    LOG.info("No job available to recover");
                }

            } catch (final Exception e) {
                e.printStackTrace();

            }
        });

    }

    @Test
    public void executeJob() throws Exception {
        assumeThat("no jobs in progress", 1, is(0)); // This is not a valid test until we can set up the in-progress job.

        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                jobWorkers.execute(recoverInterruptedJob);
                recoverInterruptedJob.setOnSucceeded(event -> {
                    if (!recoverInterruptedJob.isJobFailed()) {
                        successFlag = true;
                    }
                    latch.countDown();
                });
                recoverInterruptedJob.setOnFailed(event -> latch.countDown());
                recoverInterruptedJob.setOnCancelled(event -> latch.countDown());

            } catch (final Exception e) {
                e.printStackTrace();
                latch.countDown();
            }
        });
        latch.await();
        assertTrue(successFlag);
    }


}
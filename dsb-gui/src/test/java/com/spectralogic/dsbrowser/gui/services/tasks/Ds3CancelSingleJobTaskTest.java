package com.spectralogic.dsbrowser.gui.services.tasks;

import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.interruptedjobwindow.EndpointInfo;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.FilesAndFolderMap;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.newSessionService.SessionModelService;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedCredentials;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSession;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.util.DeepStorageBrowserTaskProgressView;
import com.spectralogic.dsbrowser.gui.util.ResourceBundleProperties;
import com.spectralogic.dsbrowser.gui.util.SessionConstants;
import com.spectralogic.dsbrowser.gui.util.StringConstants;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.shape.Circle;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

/**
 * THERE MUST BE AN INTERRUPTED JOB IN LOCAL FILE SYSTEM TO SUCCESSFULLY RUN THIS TEST CASE
 */
public class Ds3CancelSingleJobTaskTest {

    private final static Logger LOG = LoggerFactory.getLogger(Ds3CancelSingleJobTaskTest.class);
    private static final Workers workers = new Workers();
    private static Session session;
    private boolean successFlag = false;


    @BeforeClass
    public static void setConnection() {
        new JFXPanel();
        Platform.runLater(() -> {
            try {
                final SavedSession savedSession = new SavedSession(SessionConstants.SESSION_NAME, SessionConstants.SESSION_PATH, SessionConstants.PORT_NO, null, new SavedCredentials(SessionConstants.ACCESS_ID, SessionConstants.SECRET_KEY), false);
                session = new CreateConnectionTask().createConnection(SessionModelService.setSessionModel(savedSession, false));
            } catch (final Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Test
    public void call() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                final Ds3Client ds3Client = session.getClient();
                final DeepStorageBrowserPresenter deepStorageBrowserPresenter = Mockito.mock(DeepStorageBrowserPresenter.class);
                Mockito.when(deepStorageBrowserPresenter.getCircle()).thenReturn(Mockito.mock(Circle.class));
                Mockito.when(deepStorageBrowserPresenter.getLblCount()).thenReturn(Mockito.mock(Label.class));
                Mockito.when(deepStorageBrowserPresenter.getJobButton()).thenReturn(Mockito.mock(Button.class));
                final Ds3Common ds3Common = Mockito.mock(Ds3Common.class);
                Mockito.when(ds3Common.getCurrentSession()).thenReturn(session);
                final JobInterruptionStore jobInterruptionStore = JobInterruptionStore.loadJobIds();
                final Optional<Map<String, Map<String, FilesAndFolderMap>>> endPointsMap = jobInterruptionStore.getJobIdsModel()
                        .getEndpoints().stream().filter(endpoint -> endpoint.containsKey(session.getEndpoint() + StringConstants
                                .COLON + session.getPortNo())).findFirst();
                if (endPointsMap.isPresent()) {
                    final Map<String, FilesAndFolderMap> stringFilesAndFolderMapMap = endPointsMap.get().get(session.getEndpoint() + StringConstants.COLON + session.getPortNo());
                    final Optional<String> jobIdKeyElement = stringFilesAndFolderMapMap.entrySet().stream()
                            .map(Map.Entry::getKey)
                            .findFirst();
                    final EndpointInfo endPointInfo = new EndpointInfo(session.getEndpoint() + StringConstants.COLON + session.getPortNo(),
                            ds3Client,
                            stringFilesAndFolderMapMap,
                            deepStorageBrowserPresenter,
                            ds3Common
                    );
                    final DeepStorageBrowserTaskProgressView<Ds3JobTask> taskProgressView = new DeepStorageBrowserTaskProgressView<>();
                    Mockito.when(deepStorageBrowserPresenter.getJobProgressView()).thenReturn(taskProgressView);
                    if (jobIdKeyElement.isPresent()) {
                        final Ds3CancelSingleJobTask ds3CancelSingleJobTask = new Ds3CancelSingleJobTask(jobIdKeyElement.get(), endPointInfo, jobInterruptionStore, ResourceBundleProperties.getResourceBundle().getString("recover"));
                        workers.execute(ds3CancelSingleJobTask);
                        ds3CancelSingleJobTask.setOnSucceeded(event -> {
                            successFlag = true;
                            latch.countDown();

                        });
                        ds3CancelSingleJobTask.setOnFailed(event -> latch.countDown());
                        ds3CancelSingleJobTask.setOnCancelled(event -> latch.countDown());
                    } else {
                        LOG.info("No job available to cancel");
                        latch.countDown();
                    }
                } else {
                    LOG.info("No job available to cancel");
                    latch.countDown();
                }

            } catch (final Exception e) {
                e.printStackTrace();
                latch.countDown();
            }
        });
        latch.await();
        Assert.assertTrue(successFlag);
    }


}
package com.spectralogic.dsbrowser.gui.util;

import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.models.JobRequestType;
import com.spectralogic.ds3client.models.Priority;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3PutJob;
import com.spectralogic.dsbrowser.gui.components.newsession.NewSessionPresenter;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.FilesAndFolderMap;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobIdsModel;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedCredentials;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSession;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.settings.SettingsStore;
import com.spectralogic.dsbrowser.gui.services.tasks.CancelAllTaskBySession;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;

import static org.junit.Assert.*;


public class ParseJobInterruptionMapTest {
    private static final JobWorkers jobWorkers = new JobWorkers(10);
    private static final Workers workers = new Workers();
    private static Session session;
    private static String endpoint;
    private static File file;
    private static final UUID jobId = UUID.randomUUID();
    private static JobInterruptionStore jobInterruptionStore;


    @BeforeClass
    public static void setUp() throws Exception {
        new JFXPanel();
        Platform.runLater(() -> {
            try {
                //Initiating session
                final SavedSession savedSession = new SavedSession("Test1", "192.168.6.164", "8080", null, new SavedCredentials("c3VsYWJoamFpbg==", "yVBAvWTG"), false);
                session = new NewSessionPresenter().createConnection(savedSession);
                //Initializing endpoint
                endpoint = session.getEndpoint() + StringConstants.COLON + session.getPortNo();
                //Loading resource file
                final ClassLoader classLoader = ParseJobInterruptionMapTest.class.getClassLoader();
                final URL url = classLoader.getResource("files/demo.mp4");
                if (url != null) {
                    ParseJobInterruptionMapTest.file = new File(url.getFile());
                }
                final Map<String, Path> filesMap = new HashMap<>();
                filesMap.put("demoFile.txt", file.toPath());
                //Storing a interrupted job into resource file
                final FilesAndFolderMap filesAndFolderMap = new FilesAndFolderMap(filesMap, new HashMap<>(), JobRequestType.PUT.toString(), "2/03/2017 17:26:31", false, "additional", 2567L, "demo");
                final Map<String, FilesAndFolderMap> jobIdMap = new HashMap<>();
                jobIdMap.put(jobId.toString(), filesAndFolderMap);
                final Map<String, Map<String, FilesAndFolderMap>> endPointMap = new HashMap<>();
                endPointMap.put(session.getEndpoint() + ":" + session.getPortNo(), jobIdMap);
                final ArrayList<Map<String, Map<String, FilesAndFolderMap>>> endpointMapList = new ArrayList<>();
                endpointMapList.add(endPointMap);
                final JobIdsModel jobIdsModel = new JobIdsModel(endpointMapList);
                final JobInterruptionStore jobInterruptionStore1 = new JobInterruptionStore(jobIdsModel);
                jobInterruptionStore1.saveJobInterruptionStore(jobInterruptionStore1);
                jobInterruptionStore = JobInterruptionStore.loadJobIds();
            } catch (final IOException io) {
                io.printStackTrace();
            }
        });
        Thread.sleep(5000);
    }

    @Test
    public void saveValuesToFiles() throws Exception {
        Platform.runLater(() -> {
            try {
                final Map<String, Path> filesMap = new HashMap<>();
                filesMap.put("demoFile.txt", file.toPath());
                //Creating jobIdMap
                final FilesAndFolderMap filesAndFolderMap = new FilesAndFolderMap(filesMap, new HashMap<>(), JobRequestType.PUT.toString(), "2/03/2017 17:26:31", false, "additional", 2567L, "demo");
                final Map<String, FilesAndFolderMap> jobIdMap = new HashMap<>();
                jobIdMap.put(jobId.toString(), filesAndFolderMap);
                final Map<String, Map<String, FilesAndFolderMap>> endPointMap = new HashMap<>();
                endPointMap.put(session.getEndpoint() + ":" + session.getPortNo(), jobIdMap);
                //Getting endpoints
                final ArrayList<Map<String, Map<String, FilesAndFolderMap>>> endpointMapList = new ArrayList<>();
                endpointMapList.add(endPointMap);
                final JobIdsModel jobIdsModel = new JobIdsModel(endpointMapList);
                final JobInterruptionStore jobInterruptionStore1 = new JobInterruptionStore(jobIdsModel);
                jobInterruptionStore1.saveJobInterruptionStore(jobInterruptionStore1);
                final JobInterruptionStore jobInterruptionStore = JobInterruptionStore.loadJobIds();
                //Getting updated endpoints
                final Map<String, Map<String, FilesAndFolderMap>> endPointsMap = jobInterruptionStore.getJobIdsModel()
                        .getEndpoints().stream().filter(endpoint -> endpoint.containsKey(session.getEndpoint() + StringConstants
                                .COLON + session.getPortNo())).findFirst().orElse(null);
                final Map<String, FilesAndFolderMap> stringFilesAndFolderMapMap = endPointsMap.get(session.getEndpoint() + StringConstants.COLON + session.getPortNo());
                final String jobIdKey = stringFilesAndFolderMapMap.entrySet().stream()
                        .map(Map.Entry::getKey).filter(uuidKey -> uuidKey.equals(jobId.toString()))
                        .findFirst()
                        .orElse(null);
                //Validating test case
                assertEquals(jobIdKey, jobId.toString());
            } catch (final IOException io) {
                io.printStackTrace();
            }

        });
        Thread.sleep(2000);
    }

    @Test
    public void getJobIDMap() throws Exception {
        Platform.runLater(() -> {
            try {
                final Map<String, FilesAndFolderMap> jobIDMap = ParseJobInterruptionMap.getJobIDMap(
                        jobInterruptionStore.getJobIdsModel().getEndpoints(), endpoint,
                        new MyTaskProgressView<>(), null);
                final Map<String, Map<String, FilesAndFolderMap>> filesAndFolderMap1 = jobInterruptionStore
                        .getJobIdsModel()
                        .getEndpoints().get(0);
                //Get root key of interrupted jobs id map
                final String resultMapKay = filesAndFolderMap1.entrySet().stream()
                        .map(Map.Entry::getKey)
                        .findFirst()
                        .orElse(null);
                boolean result = true;
                //Check if all keys/jobs id are equal
                if (jobIDMap.size() != filesAndFolderMap1.get(resultMapKay).size()) {
                    result = false;
                } else {
                    for (final String key : jobIDMap.keySet()) {
                        if (!jobIDMap.get(key).equals(filesAndFolderMap1.get(resultMapKay).get(key))) {
                            result = false;
                        }
                    }
                }
                assertEquals(true, result);
            } catch (final Exception e) {
                e.printStackTrace();
            }
        });
        Thread.sleep(2000);

    }

    @Test
    public void removeJobIdFromFile() throws Exception {
        Platform.runLater(() -> {
            try {
                final Map<String, Path> filesMap = new HashMap<>();
                filesMap.put("demoFile.txt", file.toPath());
                //Creating jobIdMap
                final FilesAndFolderMap filesAndFolderMap = new FilesAndFolderMap(filesMap, new HashMap<>(), JobRequestType.PUT.toString(), "2/03/2017 17:26:31", false, "additional", 2567L, "demo");
                final Map<String, FilesAndFolderMap> jobIdMap = new HashMap<>();
                jobIdMap.put(jobId.toString(), filesAndFolderMap);
                final Map<String, Map<String, FilesAndFolderMap>> endPointMap = new HashMap<>();
                endPointMap.put(session.getEndpoint() + ":" + session.getPortNo(), jobIdMap);
                final ArrayList<Map<String, Map<String, FilesAndFolderMap>>> endpointMapList = new ArrayList<>();
                endpointMapList.add(endPointMap);
                final JobIdsModel jobIdsModel = new JobIdsModel(endpointMapList);
                //Saving an interrupted job to file to make method independent
                final JobInterruptionStore jobInterruptionStore1 = new JobInterruptionStore(jobIdsModel);
                jobInterruptionStore1.saveJobInterruptionStore(jobInterruptionStore1);
                jobInterruptionStore = JobInterruptionStore.loadJobIds();
                final Map<String, Map<String, FilesAndFolderMap>> endPointsMap = jobInterruptionStore.getJobIdsModel()
                        .getEndpoints().stream().filter(endpoint1 -> endpoint1.containsKey(session.getEndpoint() +
                                StringConstants.COLON + session.getPortNo())).findFirst().orElse(null);
                //Getting jobId which to be removed
                final String resultMapKay = endPointsMap.entrySet().stream()
                        .map(Map.Entry::getKey)
                        .findFirst()
                        .orElse(null);
                final String jobIdToBeRemoved = endPointsMap.get(resultMapKay).entrySet().stream()
                        .map(Map.Entry::getKey)
                        .findFirst()
                        .orElse(null);
                //Removing job from file
                ParseJobInterruptionMap.removeJobIdFromFile(jobInterruptionStore, jobIdToBeRemoved, session.getEndpoint() + StringConstants.COLON + session.getPortNo());
                jobInterruptionStore = JobInterruptionStore.loadJobIds();
                final Map<String, Map<String, FilesAndFolderMap>> endPointsMapNew = jobInterruptionStore
                        .getJobIdsModel()
                        .getEndpoints().stream().filter(endpoint1 -> endpoint1.containsKey(session.getEndpoint() +
                                StringConstants
                                        .COLON + session.getPortNo())).findFirst().orElse(null);
                final String resultMapKayNew = endPointsMapNew.entrySet().stream()
                        .map(Map.Entry::getKey)
                        .findFirst()
                        .orElse(null);
                final String jobIdToBeRemovedNew = endPointsMapNew.get(resultMapKayNew).entrySet().stream()
                        .map(Map.Entry::getKey)
                        .findFirst()
                        .orElse(null);
                //Validating test case
                assertNull(jobIdToBeRemovedNew);
            } catch (final Exception e) {
                e.printStackTrace();
            }
        });
        Thread.sleep(12000);

    }

    @Test
    public void cancelTasks() throws Exception {
        Platform.runLater(() -> {
            final List<File> filesList = new ArrayList<>();
            filesList.add(file);
            final Ds3Client ds3Client = session.getClient();
            final DeepStorageBrowserPresenter deepStorageBrowserPresenter = Mockito.mock(DeepStorageBrowserPresenter.class);
            try {
                //Initiating a put job which to be cancelled
                final SettingsStore settingsStore = SettingsStore.loadSettingsStore();
                final Ds3PutJob ds3PutJob = new Ds3PutJob(ds3Client, filesList, "TEST1", "",
                        deepStorageBrowserPresenter, Priority.URGENT.toString(), 5,
                        JobInterruptionStore.loadJobIds(), null, settingsStore);
                //Starting put job task
                jobWorkers.execute(ds3PutJob);
                ds3PutJob.setOnSucceeded(event -> {
                    System.out.println("Put job success");
                });
                Thread.sleep(5000);
                //Cancelling put job task
                final CancelRunningJobsTask cancelRunningJobsTask = ParseJobInterruptionMap.cancelTasks(jobWorkers, JobInterruptionStore.loadJobIds(), workers);
                //Validating test case
                assertNotNull(cancelRunningJobsTask);
                cancelRunningJobsTask.setOnSucceeded(event -> {
                    assertEquals(true, true);
                });
                cancelRunningJobsTask.setOnFailed(event -> {
                    fail();
                });
            } catch (final Exception e) {
                e.printStackTrace();
            }
        });
        Thread.sleep(20000);
    }

    @Test
    public void cancelAllRunningJobsBySession() throws Exception {
        Platform.runLater(() -> {
            final List<File> filesList = new ArrayList<>();
            filesList.add(file);
            final Ds3Client ds3Client = session.getClient();
            final DeepStorageBrowserPresenter deepStorageBrowserPresenter = Mockito.mock(DeepStorageBrowserPresenter.class);
            try {
                //Initiating put job which to be cancelled
                final SettingsStore settingsStore = SettingsStore.loadSettingsStore();
                final Ds3PutJob ds3PutJob = new Ds3PutJob(ds3Client, filesList, "TEST1", "",
                        deepStorageBrowserPresenter, Priority.URGENT.toString(), 5,
                        JobInterruptionStore.loadJobIds(), null, settingsStore);
                //Starting put job task
                jobWorkers.execute(ds3PutJob);
                ds3PutJob.setOnSucceeded(event -> {
                    System.out.println("Put job success");
                });
                Thread.sleep(5000);
                //Cancelling task by session
                final CancelAllTaskBySession cancelAllRunningJobsBySession = ParseJobInterruptionMap.cancelAllRunningJobsBySession(jobWorkers,
                        jobInterruptionStore, null, workers, session);
                //Validating test case
                assertNotNull(cancelAllRunningJobsBySession);
                cancelAllRunningJobsBySession.setOnSucceeded(event -> {
                    assertEquals(true, true);
                });
                cancelAllRunningJobsBySession.setOnFailed(event -> {
                    fail();
                });
            } catch (final Exception e) {
                e.printStackTrace();
            }
        });
        Thread.sleep(20000);
    }
}
package com.spectralogic.dsbrowser.gui;

import com.google.inject.Injector;
import com.spectralogic.dsbrowser.gui.injector.GuicePresenterInjector;
import com.spectralogic.dsbrowser.gui.services.logservice.ApplicationLoggerSettings;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    public static void main(final String[] args) {
        launch(args);
    }

    @Override
    public void start(final Stage primaryStage) throws Exception {
        final Injector injector = GuicePresenterInjector.injector;

        // restoring the logging settings before any other components are loaded
        final ApplicationLoggerSettings applicationLoggerSettings = injector.getInstance(ApplicationLoggerSettings.class);
        applicationLoggerSettings.restoreLoggingSettings();

        final DeepStorageBrowser dsb = injector.getInstance(DeepStorageBrowser.class);
        dsb.start(primaryStage);
    }
}

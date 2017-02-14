package com.spectralogic.dsbrowser.gui.services.settings;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.spectralogic.dsbrowser.gui.util.StringConstants;
import javafx.beans.property.*;

import java.nio.file.Paths;

public class LogSettings {

    public static final LogSettings DEFAULT = createDefault();

    @JsonProperty("logLocation")
    private final StringProperty logLocation = new SimpleStringProperty();

    @JsonProperty("logSize")
    private final IntegerProperty logSize = new SimpleIntegerProperty();

    @JsonProperty("numRollovers")
    private final IntegerProperty numRollovers = new SimpleIntegerProperty();

    @JsonProperty("debugLogging")
    private final BooleanProperty debugLogging = new SimpleBooleanProperty();

    @JsonProperty("consoleLogging")
    private final BooleanProperty consoleLogging = new SimpleBooleanProperty();

    public LogSettings(final String logLocation, final int logSize, final int numRollovers,
                       final boolean debugLogging, final boolean consoleLogging) {
        this.logLocation.set(logLocation);
        this.logSize.set(logSize);
        this.numRollovers.set(numRollovers);
        this.debugLogging.set(debugLogging);
        this.consoleLogging.set(consoleLogging);
    }

    public LogSettings() {
        //Default constructor needed
    }

    public static LogSettings createDefault() {
        final String logPath = Paths.get(System.getProperty(StringConstants.SETTING_FILE_PATH),
                StringConstants.SETTING_FILE_FOLDER_NAME, StringConstants.LOG).toString();
        return new LogSettings(logPath, 1, 10, true, false);
    }

    public LogSettings copy() {
        final LogSettings settings = new LogSettings();
        settings.setLogLocation(this.getLogLocation());
        settings.setLogSize(this.getLogSize());
        settings.setNumRollovers(this.getNumRollovers());
        settings.setDebugLogging(this.getDebugLogging());
        settings.setConsoleLogging(this.getConsoleLogging());
        return settings;
    }

    public void overwrite(final LogSettings settings) {
        this.setLogLocation(settings.getLogLocation());
        this.setLogSize(settings.getLogSize());
        this.setNumRollovers(settings.getNumRollovers());
        this.setDebugLogging(settings.getDebugLogging());
        this.setConsoleLogging(settings.getConsoleLogging());
    }

    public String getLogLocation() {
        return logLocation.get();
    }

    public void setLogLocation(final String logLocation) {
        this.logLocation.set(logLocation);
    }

    public StringProperty getLogLocationProperty() {
        return logLocation;
    }

    public int getLogSize() {
        return logSize.get();
    }

    public void setLogSize(final int logSize) {
        this.logSize.set(logSize);
    }

    public IntegerProperty getLogSizeProperty() {
        return logSize;
    }

    public int getNumRollovers() {
        return numRollovers.get();
    }

    public void setNumRollovers(final int numRollovers) {
        this.numRollovers.set(numRollovers);
    }

    public IntegerProperty getNumRolloversProperty() {
        return numRollovers;
    }

    public boolean getDebugLogging() {
        return debugLogging.get();
    }

    public void setDebugLogging(final boolean debugLogging) {
        this.debugLogging.set(debugLogging);
    }

    public BooleanProperty getDebugLoggingProperty() {
        return debugLogging;
    }

    public boolean getConsoleLogging() {
        return consoleLogging.get();
    }

    public void setConsoleLogging(final boolean consoleLogging) {
        this.consoleLogging.set(consoleLogging);
    }

    public BooleanProperty getConsoleLoggingProperty() {
        return consoleLogging;
    }
}

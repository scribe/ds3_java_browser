package com.spectralogic.dsbrowser.gui.util;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class LazyAlert {

    private final String title;
    private final Alert.AlertType alertType;
    private Alert alert = null;

    public LazyAlert(final String title, final Alert.AlertType alertType) {
        this.title = title;
        this.alertType = alertType;
    }

    public LazyAlert(final String title) {
        this(title, Alert.AlertType.INFORMATION);
    }

    private void showAlertInternal(final String message) {
        if (alert == null) {
            alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setHeaderText(null);

            final Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.getIcons().add(new Image(ImageURLs.DEEPSTORAGEBROWSER));

        }
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void showAlert(final String message) {
        if (Platform.isFxApplicationThread()) {
            showAlertInternal(message);
        } else {
            Platform.runLater(() -> showAlertInternal(message));
        }
    }
}

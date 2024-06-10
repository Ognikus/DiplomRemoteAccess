package com.example.diplomremoteaccess.controlller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

public class AccessRequestController {

    @FXML
    private ImageView profileImage;
    @FXML
    private Label usernameLabel;
    @FXML
    private Label requestMessage;
    @FXML
    private Button keyboardPermission;
    @FXML
    private Button clipboardPermission;
    @FXML
    private Button soundPermission;
    @FXML
    private Button mousePermission;
    @FXML
    private Button screenPermission;
    @FXML
    private Button filesPermission;

    @FXML
    public void initialize() {
        // Set profile image
        profileImage.setImage(new Image("path/to/profile/image.png"));

        // Set initial permissions (could be set based on previous choices or default values)
        keyboardPermission.setStyle("-fx-background-color: lightgray;");
        clipboardPermission.setStyle("-fx-background-color: lightgray;");
        soundPermission.setStyle("-fx-background-color: lightgray;");
        mousePermission.setStyle("-fx-background-color: lightgray;");
        screenPermission.setStyle("-fx-background-color: lightgray;");
        filesPermission.setStyle("-fx-background-color: lightgray;");
    }

    @FXML
    private void handleAccept() {
        // Handle the accept action
        System.out.println("Access accepted");
        closeWindow();
    }

    @FXML
    private void handleAcceptAndElevate() {
        // Handle the accept and elevate action
        System.out.println("Access accepted and elevated");
        closeWindow();
    }

    @FXML
    private void handleCancel() {
        // Handle the cancel action
        System.out.println("Access canceled");
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) profileImage.getScene().getWindow();
        stage.close();
    }
}


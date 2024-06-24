package com.example.diplomremoteaccess.controlller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.awt.event.ActionEvent;

public class AccessRequestController {

    @FXML
    private Button clipboardPermission;

    @FXML
    private Button filesPermission;

    @FXML
    private Button keyboardPermission;

    @FXML
    private Button mousePermission;

    @FXML
    private ImageView profileImage;

    @FXML
    private Label requestMessage;

    @FXML
    private Button screenPermission;

    @FXML
    private Label usernameLabel;

    @FXML
    void handleAccept(ActionEvent event) {

    }

    @FXML
    void handleCancel(ActionEvent event) {

    }

    @FXML
    public void initialize() {
        // Установить изображение ОС
        profileImage.setImage(new Image("path/to/profile/image.png"));

        // Установить начальные разрешения
        keyboardPermission.setStyle("-fx-background-color: lightgray;");
        clipboardPermission.setStyle("-fx-background-color: lightgray;");
        mousePermission.setStyle("-fx-background-color: lightgray;");
        screenPermission.setStyle("-fx-background-color: lightgray;");
        filesPermission.setStyle("-fx-background-color: lightgray;");
    }

    @FXML
    private void handleAccept() {
        // Обработать действие "Принять"
        System.out.println("Access accepted");
        closeWindow();
    }

    @FXML
    private void handleCancel() {
        // Обработать действие отмены
        System.out.println("Access canceled");
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) profileImage.getScene().getWindow();
        stage.close();
    }
}


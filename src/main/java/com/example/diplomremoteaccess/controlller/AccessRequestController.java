package com.example.diplomremoteaccess.controlller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.java_websocket.WebSocket;

public class AccessRequestController {

    @FXML
    private Label usernameLabel;
    @FXML
    private Label requestMessage;
    @FXML
    private Button keyboardPermission;
    @FXML
    private Button clipboardPermission;
    @FXML
    private Button mousePermission;
    @FXML
    private Button screenPermission;
    @FXML
    private Button filesPermission;
    @FXML
    private Button BtnAccept;
    @FXML
    private Button BtnCancel;

    private WebSocket conn;
    private boolean allowKeyboard = true;
    private boolean allowClipboard = true;
    private boolean allowMouse = true;
    private boolean allowScreen = true;
    private boolean allowFiles = true;

    public void setConnection(WebSocket conn) {
        this.conn = conn;
        usernameLabel.setText("Запрос на подключение от: " + conn.getRemoteSocketAddress().getAddress());
    }

    @FXML
    private void initialize() {
        keyboardPermission.setOnAction(event -> togglePermission(keyboardPermission, "keyboard"));
        clipboardPermission.setOnAction(event -> togglePermission(clipboardPermission, "clipboard"));
        mousePermission.setOnAction(event -> togglePermission(mousePermission, "mouse"));
        screenPermission.setOnAction(event -> togglePermission(screenPermission, "screen"));
        filesPermission.setOnAction(event -> togglePermission(filesPermission, "files"));
        BtnAccept.setOnAction(event -> acceptConnection());
        BtnCancel.setOnAction(event -> declineConnection());
    }

    private void togglePermission(Button button, String permission) {
        boolean isEnabled;
        switch (permission) {
            case "keyboard":
                isEnabled = allowKeyboard = !allowKeyboard;
                break;
            case "clipboard":
                isEnabled = allowClipboard = !allowClipboard;
                break;
            case "mouse":
                isEnabled = allowMouse = !allowMouse;
                break;
            case "screen":
                isEnabled = allowScreen = !allowScreen;
                break;
            case "files":
                isEnabled = allowFiles = !allowFiles;
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + permission);
        }
        button.setStyle(isEnabled ? "-fx-background-color: green;" : "-fx-background-color: red;");
    }

    @FXML
    private void acceptConnection() {
        conn.send("PASSWORD_OK");
        conn.send("PERMISSIONS " + allowKeyboard + " " + allowClipboard + " " + allowMouse + " " + allowScreen + " " + allowFiles);
    }

    @FXML
    private void declineConnection() {
        conn.send("PASSWORD_FAIL");
        conn.close();
        // Закрываем только это окно
        Stage stage = (Stage) BtnCancel.getScene().getWindow();
        stage.close();
    }
}


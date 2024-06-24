package com.example.diplomremoteaccess.controlller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

public class FileTransferController {

    @FXML
    private Button BtnOpenLocalFolder;
    @FXML
    private Button BtnUpdateLocalFolder;
    @FXML
    private Button BtnSendFile;
    @FXML
    private Button BtnReceiveFile;
    @FXML
    private Button BtnOpenRemoteFolder;
    @FXML
    private Button BtnUpdateRemoteFolder;
    @FXML
    private ListView<String> localFileListView;
    @FXML
    private ListView<String> remoteFileListView;

    private WebSocketClient client;
    private File currentLocalDirectory;
    private File currentRemoteDirectory;
    private URI serverUri;
    private Stage stage;

    public FileTransferController() {
    }

    public FileTransferController(URI serverUri, Stage stage) {
        this.serverUri = serverUri;
        this.stage = stage;
    }

    public void setServerUri(URI serverUri) {
        this.serverUri = serverUri;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    public void initialize() {
        BtnOpenLocalFolder.setOnAction(e -> chooseLocalDirectory());
        BtnUpdateLocalFolder.setOnAction(e -> updateLocalFileList());
        BtnSendFile.setOnAction(e -> uploadFile());

        BtnOpenRemoteFolder.setOnAction(e -> chooseRemoteDirectory());
        BtnUpdateRemoteFolder.setOnAction(e -> updateRemoteFileList());
        BtnReceiveFile.setOnAction(e -> downloadFile());
    }

    @FXML
    private void chooseLocalDirectory() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Выберите локальную папку");
        File selectedDirectory = directoryChooser.showDialog(stage);
        if (selectedDirectory != null) {
            currentLocalDirectory = selectedDirectory;
            updateLocalFileList();
        }
    }

    @FXML
    private void chooseRemoteDirectory() {
        // Реализовать выбор директории на удаленном компьютере
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Выберите удаленную папку");
        File selectedDirectory = directoryChooser.showDialog(stage);
        if (selectedDirectory != null) {
            currentRemoteDirectory = selectedDirectory;
            updateRemoteFileList();
        }
    }

    private void updateLocalFileList() {
        localFileListView.getItems().clear();
        if (currentLocalDirectory != null && currentLocalDirectory.isDirectory()) {
            for (File file : currentLocalDirectory.listFiles()) {
                localFileListView.getItems().add(file.getName());
            }
        }
    }

    private void updateRemoteFileList() {
        // Обновить список файлов на удаленном компьютере
        remoteFileListView.getItems().clear();
        if (currentRemoteDirectory != null && currentRemoteDirectory.isDirectory()) {
            for (File file : currentRemoteDirectory.listFiles()) {
                remoteFileListView.getItems().add(file.getName());
            }
        }
    }

    private void uploadFile() {
        String selectedFile = localFileListView.getSelectionModel().getSelectedItem();
        if (selectedFile != null && currentLocalDirectory != null) {
            File fileToUpload = new File(currentLocalDirectory, selectedFile);
            sendFile(fileToUpload);
        } else {
            showAlert("Ошибка", "Выберите файл для загрузки.");
        }
    }

    private void downloadFile() {
        String selectedFile = remoteFileListView.getSelectionModel().getSelectedItem();
        if (selectedFile != null && currentRemoteDirectory != null) {
            requestFile(selectedFile);
        } else {
            showAlert("Ошибка", "Выберите файл для скачивания.");
        }
    }

    private void sendFile(File file) {
        try {
            byte[] fileContent = Files.readAllBytes(file.toPath());
            client.send("UPLOAD " + file.getName() + " " + Base64.getEncoder().encodeToString(fileContent));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void requestFile(String fileName) {
        client.send("DOWNLOAD " + fileName);
    }

    private void receiveFile(String fileName) {
        try {
            byte[] fileContent = Base64.getDecoder().decode(fileName);
            Path path = new File(currentLocalDirectory, "received_" + fileName).toPath();
            Files.write(path, fileContent);
            System.out.println("Полученный файл: " + fileName);
            updateLocalFileList();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public void connect() {
        try {
            client = new WebSocketClient(serverUri) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    Platform.runLater(() -> showAlert("Соединение установлено", "Подключено к серверу передачи файлов."));
                }

                @Override
                public void onMessage(String message) {
                    if (message.startsWith("FILE ")) {
                        String[] parts = message.split(" ", 2);
                        String fileName = parts[1];
                        receiveFile(fileName);
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    Platform.runLater(() -> showAlert("Соединение закрыто", "Отключено от сервера передачи файлов."));
                }

                @Override
                public void onError(Exception ex) {
                    ex.printStackTrace();
                }
            };
            client.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

package com.example.diplomremoteaccess.controlller;


import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class FileTransferController {

    @FXML
    private TextField localPathField;
    @FXML
    private TextField remotePathField;
    @FXML
    private ListView<String> localFileList;
    @FXML
    private ListView<String> remoteFileList;
    @FXML
    private Button handleSend;
    @FXML
    private Button handleReceive;

    @FXML
    public void initialize() {
        // Инициализация значений (если необходимо)
    }

    @FXML
    private void handleRefreshLocal() {
        // Логика обновления списка локальных файлов
        String localPath = localPathField.getText();
        // Получение списка файлов и добавление в localFileList
    }

    @FXML
    private void handleRefreshRemote() {
        // Логика обновления списка удаленных файлов
        String remotePath = remotePathField.getText();
        // Получение списка файлов и добавление в remoteFileList
    }

    @FXML
    private void handleSend() {
        // Логика отправки файлов на удаленный компьютер
        String selectedFile = localFileList.getSelectionModel().getSelectedItem();
        if (selectedFile != null) {
            // Логика отправки файла
        }
    }

    @FXML
    private void handleReceive() {
        // Логика получения файлов с удаленного компьютера
        String selectedFile = remoteFileList.getSelectionModel().getSelectedItem();
        if (selectedFile != null) {
            // Логика получения файла
        }
    }

    @FXML
    private void handleClose() {
        // Закрытие окна
        Stage stage = (Stage) handleSend.getScene().getWindow();
        stage.close();
    }
}


package com.example.diplomremoteaccess.remote.client;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import javax.swing.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.Base64;

public class FileTransferClient extends WebSocketClient {

    public FileTransferClient(URI serverUri) {
        super(serverUri);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("Подключен к серверу передачи файлов");
        // Показать диалоговое окно передачи файла
        SwingUtilities.invokeLater(this::showFileTransferDialog);
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
        System.out.println("Отключен от сервера передачи файлов");
    }

    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
    }

    private void showFileTransferDialog() {
        JFrame frame = new JFrame("Передача файлов");
        frame.setSize(400, 300);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        JPanel panel = new JPanel();
        frame.add(panel);
        placeComponents(panel);
        frame.setVisible(true);
    }

    private void placeComponents(JPanel panel) {
        panel.setLayout(null);

        JLabel userLabel = new JLabel("Выберите файл:");
        userLabel.setBounds(10, 20, 80, 25);
        panel.add(userLabel);

        JTextField fileTextField = new JTextField(20);
        fileTextField.setBounds(100, 20, 165, 25);
        panel.add(fileTextField);

        JButton chooseFileButton = new JButton("Просматривать");
        chooseFileButton.setBounds(270, 20, 80, 25);
        panel.add(chooseFileButton);

        chooseFileButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int returnValue = fileChooser.showOpenDialog(null);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                fileTextField.setText(selectedFile.getAbsolutePath());
            }
        });

        JButton sendFileButton = new JButton("Отправить файл");
        sendFileButton.setBounds(10, 80, 150, 25);
        panel.add(sendFileButton);

        sendFileButton.addActionListener(e -> {
            String filePath = fileTextField.getText();
            if (!filePath.isEmpty()) {
                sendFile(new File(filePath));
            }
        });
    }

    private void sendFile(File file) {
        try {
            byte[] fileContent = Files.readAllBytes(file.toPath());
            send("FILE " + file.getName() + " " + Base64.getEncoder().encodeToString(fileContent));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receiveFile(String fileName) {
        try {
            byte[] fileContent = Base64.getDecoder().decode(fileName);
            FileOutputStream fos = new FileOutputStream(new File("received_" + fileName));
            fos.write(fileContent);
            fos.close();
            System.out.println("Полученный файл: " + fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

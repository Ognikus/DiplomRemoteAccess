package com.example.diplomremoteaccess.controlller;

import com.example.diplomremoteaccess.remote.client.Client;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.Pane;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.Properties;

public class StartController {

    private static final String PROPERTIES_FILE = "computer_data.properties";
    private static final String ID_KEY = "computerId";
    private static final String PASSWORD_KEY = "oneTimePassword";
    private static final String NAME_KEY = "computerName";
    private static final String IP_KEY = "computerIP";

    private WebSocketClient client;
    private JFrame frame;
    private JLabel imageLabel;

    @FXML
    private Button BtnConnectPc;
    @FXML
    private Button BtnSettingsPage;
    @FXML
    private Button BtnStartPage;
    @FXML
    private Button BtnUpdatePassword;
    @FXML
    private TextField FieldIdYouPC;
    @FXML
    private TextField FieldPcForConnect;
    @FXML
    private TextField FieldTimePasswordPc;
    @FXML
    private Pane PaneConnect;
    @FXML
    private Pane PaneStartInfo;

    @FXML
    public void initialize() {
        String computerId = loadProperty(ID_KEY);
        if (computerId == null || computerId.trim().isEmpty()) {
            String computerIP = getComputerIP();
            if (!"Unknown".equals(computerIP)) {
                computerId = encodeIPToID(computerIP);
                saveProperty(ID_KEY, computerId);
                saveProperty(IP_KEY, computerIP);
            }
        }
        FieldIdYouPC.setText(computerId);

        String oneTimePassword = loadProperty(PASSWORD_KEY);
        if (oneTimePassword == null || oneTimePassword.trim().isEmpty()) {
            oneTimePassword = generateOneTimePassword();
            saveProperty(PASSWORD_KEY, oneTimePassword);
        }
        FieldTimePasswordPc.setText(oneTimePassword);

        String computerName = loadProperty(NAME_KEY);
        if (computerName == null || computerName.trim().isEmpty()) {
            computerName = getComputerName();
            saveProperty(NAME_KEY, computerName);
        }

        BtnUpdatePassword.setOnAction(event -> updatePassword());
        BtnConnectPc.setOnAction(event -> connectToRemoteComputer());
    }

    private String getComputerName() {
        return System.getProperty("user.name");
    }

    private String getComputerIP() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            e.printStackTrace();
            return "Unknown";
        }
    }

    private String encodeIPToID(String ip) {
        String[] parts = ip.split("\\.");
        StringBuilder id = new StringBuilder();
        for (String part : parts) {
            String formattedPart = String.format("%03d", Integer.parseInt(part));
            id.append(formattedPart);
        }
        return formatID(id.toString());
    }

    private String decodeIDToIP(String id) {
        id = id.replaceAll(" ", "");
        StringBuilder ip = new StringBuilder();
        for (int i = 0; i < id.length(); i += 3) {
            String part = id.substring(i, i + 3);
            int intPart = Integer.parseInt(part);
            ip.append(intPart).append(".");
        }
        return ip.toString().substring(0, ip.length() - 1);
    }

    private String formatID(String id) {
        // Ensure the ID has exactly 12 digits
        while (id.length() < 12) {
            id += "0";
        }
        return id.substring(0, 3) + " " + id.substring(3, 6) + " " + id.substring(6, 9) + " " + id.substring(9, 12);
    }

    private void saveProperty(String key, String value) {
        Properties properties = new Properties();
        try (FileInputStream in = new FileInputStream(PROPERTIES_FILE)) {
            properties.load(in);
        } catch (IOException e) {
            // файл не найден, будет создан новый
        }

        properties.setProperty(key, value);

        try (FileOutputStream out = new FileOutputStream(PROPERTIES_FILE)) {
            properties.store(out, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String loadProperty(String key) {
        Properties properties = new Properties();
        try (FileInputStream in = new FileInputStream(PROPERTIES_FILE)) {
            properties.load(in);
            return properties.getProperty(key);
        } catch (IOException e) {
            // Файл не найден или не удалось прочитать
            return null;
        }
    }

    private String generateOneTimePassword() {
        final String CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            password.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return password.toString();
    }

    @FXML
    private void updatePassword() {
        String oneTimePassword = generateOneTimePassword();
        FieldTimePasswordPc.setText(oneTimePassword);
        saveProperty(PASSWORD_KEY, oneTimePassword);
    }

    @FXML
    private void connectToRemoteComputer() {
        String remoteComputerId = FieldPcForConnect.getText().replaceAll(" ", "");

        // Decode the ID to get the IP address
        String remoteComputerIP = decodeIDToIP(remoteComputerId);

        // Initialize WebSocket client
        try {
            client = new WebSocketClient(new URI("ws://" + remoteComputerIP + ":8887")) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    System.out.println("Подключен к серверу");
                    // Show password prompt
                    Platform.runLater(() -> showPasswordPrompt());
                }

                @Override
                public void onMessage(String message) {
                    try {
                        byte[] imageBytes = Base64.getDecoder().decode(message);
                        ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
                        BufferedImage image = ImageIO.read(bais);

                        // Update the image in the JFrame
                        if (frame != null) {
                            imageLabel.setIcon(new ImageIcon(image));
                            frame.repaint();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("Отключен от сервера");
                }

                @Override
                public void onError(Exception ex) {
                    ex.printStackTrace();
                }
            };
            client.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private void showPasswordPrompt() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Введите пароль");
        dialog.setHeaderText("Введите одноразовый пароль для удаленного доступа:");
        dialog.setContentText("Пароль:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(password -> {
            if (validatePassword(password)) {
                Platform.runLater(() -> startClient(password));
            } else {
                showErrorAlert("Неверный пароль", "Введенный пароль неверен. Пожалуйста, попробуйте снова.");
            }
        });
    }

    private boolean validatePassword(String password) {
        return true; // Validation now happens on the server
    }

    private void startClient(String password) {
        try {
            String remoteComputerId = FieldPcForConnect.getText().replaceAll(" ", "");
            String remoteComputerIP = decodeIDToIP(remoteComputerId);

            Client client = new Client(new URI("ws://" + remoteComputerIP + ":8887"), password);
            client.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            showErrorAlert("Ошибка соединения", "Не удалось запустить клиент удаленного рабочего стола.");
        }
    }

    private void showErrorAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}

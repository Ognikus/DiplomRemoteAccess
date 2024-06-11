package com.example.diplomremoteaccess.controlller;


import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
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
    private static final String IP_PUBLIC_KEY = "computerPublicIP";


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
    private ListView<String> localFileList;
    @FXML
    private ListView<String> remoteFileList;
    @FXML
    private Pane PaneConnect;
    @FXML
    private Pane PaneStartInfo;

    @FXML
    public void initialize() {
        String computerId = loadProperty(ID_KEY);
        if (computerId == null || computerId.trim().isEmpty()) {
            String computerPublicIP = getPublicIP();
            String computerIP = getComputerIP();
            if (!"Unknown".equals(computerIP)) {
                computerId = encodeIPToID(computerIP);
                saveProperty(ID_KEY, computerId);
                saveProperty(IP_KEY, computerIP);
                saveProperty(IP_PUBLIC_KEY, computerPublicIP);
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

    private String getPublicIP() {
        String publicIP = "Unknown";
        try {
            URL url = new URL("https://api.ipify.org");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                publicIP = in.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return publicIP;
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
                    System.out.println("Connected to server");
                    // Show password prompt
                    Platform.runLater(() -> showPasswordPrompt());
                }

                @Override
                public void onMessage(String message) {
                    if (message.equals("PASSWORD_OK")) {
                        Platform.runLater(() -> {
                            frame = new JFrame("Remote Desktop");
                            imageLabel = new JLabel();
                            frame.add(imageLabel);
                            frame.setSize(800, 600);
                            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                            frame.setVisible(true);

                            imageLabel.addMouseMotionListener(new MouseMotionAdapter() {
                                @Override
                                public void mouseMoved(MouseEvent e) {
                                    client.send("MOUSE_MOVE " + e.getX() + " " + e.getY());
                                }

                                @Override
                                public void mouseDragged(MouseEvent e) {
                                    client.send("MOUSE_MOVE " + e.getX() + " " + e.getY());
                                }
                            });

                            imageLabel.addMouseListener(new MouseAdapter() {
                                @Override
                                public void mousePressed(MouseEvent e) {
                                    client.send("MOUSE_CLICK " + e.getButton());
                                }
                            });

                            frame.addKeyListener(new KeyAdapter() {
                                @Override
                                public void keyPressed(KeyEvent e) {
                                    client.send("KEY_PRESS " + e.getKeyCode());
                                }

                                @Override
                                public void keyReleased(KeyEvent e) {
                                    client.send("KEY_RELEASE " + e.getKeyCode());
                                }
                            });
                        });
                    } else if (message.equals("PASSWORD_FAIL")) {
                        Platform.runLater(() -> {
                            showErrorAlert("Invalid Password", "The password entered is invalid. Please try again.");
                            client.close();
                        });
                    } else {
                        try {
                            byte[] imageBytes = Base64.getDecoder().decode(message);
                            ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
                            BufferedImage image = ImageIO.read(bais);
                            ImageIcon imageIcon = new ImageIcon(image);
                            imageLabel.setIcon(imageIcon);
                            frame.repaint();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("Disconnected from server");
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
        dialog.setTitle("Enter Password");
        dialog.setHeaderText("Enter the one-time password for remote access:");
        dialog.setContentText("Password:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(password -> {
            client.send("PASSWORD " + password);
        });
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

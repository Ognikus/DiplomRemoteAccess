package com.example.diplomremoteaccess.controlller;


import com.example.diplomremoteaccess.remote.client.Client;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
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
    private static final String OS_KEY = "operatingSystem";


    private WebSocketClient client;
    private JFrame frame;
    private JLabel imageLabel;

    @FXML
    private Button BtnConnectPc;
    @FXML
    private Button BtnConnectPcFile;
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
        String operatingSystem = loadProperty(OS_KEY);
        if (operatingSystem == null || operatingSystem.trim().isEmpty()) {
            operatingSystem = getOperatingSystem();
            saveProperty(OS_KEY, operatingSystem);
        }

        BtnUpdatePassword.setOnAction(event -> updatePassword());
        BtnConnectPc.setOnAction(event -> connectToRemoteComputer());
        BtnConnectPcFile.setOnAction(event -> connectToFileTransfer());
    }

    private String getComputerName() {
        return System.getProperty("user.name");
    }

    private String getOperatingSystem() {return System.getProperty("os.name");}

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

        // Расшифруйте ID, чтобы получить IP-адрес
        String remoteComputerIP = decodeIDToIP(remoteComputerId);

        System.out.println("Попытка подключиться к: " + remoteComputerIP);  // Журнал для отладки

        // Инициализировать клиент WebSocket
        try {
            client = new WebSocketClient(new URI("ws://" + remoteComputerIP + ":8888")) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    System.out.println("Подключен к серверу");
                    Platform.runLater(() -> showPasswordPrompt());
                }

                @Override
                public void onMessage(String message) {
                    if (message.equals("PASSWORD_OK")) {
                        Platform.runLater(() -> startRemoteDesktop());
                    } else if (message.equals("PASSWORD_FAIL")) {
                        Platform.runLater(() -> {
                            showErrorAlert("Неверный пароль", "Введенный пароль неверен. Пожалуйста, попробуйте снова.");
                            client.close();
                        });
                    } else {
                        try {
                            byte[] imageBytes = Base64.getDecoder().decode(message);
                            ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
                            BufferedImage image = ImageIO.read(bais);
                            if (frame != null) {
                                imageLabel.setIcon(new ImageIcon(image));
                                frame.repaint();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }


                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("Отключен от сервера: " + reason);
                }

                @Override
                public void onError(Exception ex) {
                    ex.printStackTrace();
                }
            };
            client.connect();
            System.out.println("Инициировано подключение к веб-сокету.");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            showErrorAlert("ошибка соединения", "Неверный адрес сервера.");
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("ошибка соединения", "Не удалось установить соединение.");
        }
    }


    @FXML
    private void connectToFileTransfer() {
        String remoteComputerId = FieldPcForConnect.getText().replaceAll(" ", "");

        // Декодирование ID для получения IP-адреса
        String remoteComputerIP = decodeIDToIP(remoteComputerId);

        // Инициализация WebSocket клиента для передачи файлов
        try {
            Stage fileTransferStage = new Stage();
            URI serverUri = new URI("ws://" + remoteComputerIP + ":8888");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/diplomremoteaccess/FileTransfer.fxml"));
            Parent root = loader.load();

            // Получаем контроллер после загрузки FXML
            FileTransferController fileTransferController = loader.getController();
            fileTransferController.setServerUri(serverUri);
            fileTransferController.setStage(fileTransferStage);

            Scene scene = new Scene(root);
            fileTransferStage.setScene(scene);
            fileTransferStage.setTitle("Передача файлов");
            fileTransferStage.show();

            fileTransferController.connect();
        } catch (Exception e) {
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
        return true; // Проверка теперь происходит на сервере
    }

    private void startClient(String password) {
        try {
            String remoteComputerId = FieldPcForConnect.getText().replaceAll(" ", "");
            String remoteComputerIP = decodeIDToIP(remoteComputerId);

            Client client = new Client(new URI("ws://" + remoteComputerIP + ":8888"), password);
            client.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            showErrorAlert("Ошибка соединения", "Не удалось запустить клиент удаленного рабочего стола.");
        }
    }

    private void startRemoteDesktop() {
        frame = new JFrame("Удалённый рабочий стол");
        imageLabel = new JLabel();
        frame.add(new JScrollPane(imageLabel));  // Используем JScrollPane для поддержки прокрутки
        frame.setSize(1920, 1080);  // Устанавливаем размер окна
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
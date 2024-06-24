package com.example.diplomremoteaccess.remote.server;

import com.example.diplomremoteaccess.controlller.AccessRequestController;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Properties;

public class Server extends WebSocketServer {

    private static final String PROPERTIES_FILE = "computer_data.properties";
    private static final String PASSWORD_KEY = "oneTimePassword";

    private Robot robot;
    private Rectangle screenRect;
    private WebSocket currentConnection;

    public Server(InetSocketAddress address) {
        super(address);
        try {
            robot = new Robot();
            screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());

            // Получение разрешения основного экрана
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice[] screens = ge.getScreenDevices();
            for (GraphicsDevice screen : screens) {
                if (screen.getDefaultConfiguration().getBounds().contains(screenRect)) {
                    screenRect = screen.getDefaultConfiguration().getBounds();
                    break;
                }
            }

        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        currentConnection = conn;
        SwingUtilities.invokeLater(() -> showConnectionRequestDialog(conn));
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Закрытое соединение: " + conn.getRemoteSocketAddress() + " с кодом выхода " + code + " дополнительная информация: " + reason);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        if (message.startsWith("PASSWORD ")) {
            String password = message.split(" ")[1];
            if (validatePassword(password)) {
                conn.send("PASSWORD_OK");
                startScreenCapture(conn);
            } else {
                conn.send("PASSWORD_FAIL");
                conn.close();
            }
        } else if (message.startsWith("FILE ")) {
            String[] parts = message.split(" ", 3);
            String fileName = parts[1];
            String fileContent = parts[2];
            receiveFile(fileName, fileContent);
        } else if (message.startsWith("MOUSE_MOVE ")) {
            String[] parts = message.split(" ");
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            robot.mouseMove(x, y);
        } else if (message.startsWith("MOUSE_CLICK ")) {
            int button = Integer.parseInt(message.split(" ")[1]);
            if (button == 1) {
                robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            } else if (button == 2) {
                robot.mousePress(InputEvent.BUTTON2_DOWN_MASK);
                robot.mouseRelease(InputEvent.BUTTON2_DOWN_MASK);
            } else if (button == 3) {
                robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
                robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
            }
        } else if (message.startsWith("KEY_PRESS ")) {
            int keyCode = Integer.parseInt(message.split(" ")[1]);
            robot.keyPress(keyCode);
        } else if (message.startsWith("KEY_RELEASE ")) {
            int keyCode = Integer.parseInt(message.split(" ")[1]);
            robot.keyRelease(keyCode);
        }
        if (message.startsWith("FILE ")) {
            String[] parts = message.split(" ", 3);
            String fileName = parts[1];
            String fileContent = parts[2];
            saveFile(fileName, fileContent);
        } else if (message.startsWith("REQUEST_FILE ")) {
            String fileName = message.split(" ", 2)[1];
            sendFile(conn, fileName);
        } else if (message.equals("LIST_FILES")) {
            sendFileList(conn);
        }
    }

    private void receiveFile(String fileName, String fileContent) {
        try {
            byte[] fileData = Base64.getDecoder().decode(fileContent);
            Files.write(new File("received_" + fileName).toPath(), fileData);
            System.out.println("Полученный файл: " + fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("Сервер успешно запущен");
    }



    private void startScreenCapture(WebSocket conn) {
        new Thread(() -> {
            while (conn.isOpen()) {
                try {
                    BufferedImage screenCapture = robot.createScreenCapture(screenRect);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(screenCapture, "jpg", baos);
                    byte[] bytes = baos.toByteArray();
                    String base64Image = Base64.getEncoder().encodeToString(bytes);
                    conn.send(base64Image);
                    Thread.sleep(30);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private boolean validatePassword(String password) {
        Properties properties = new Properties();
        try (FileInputStream in = new FileInputStream(PROPERTIES_FILE)) {
            properties.load(in);
            String storedPassword = properties.getProperty(PASSWORD_KEY);
            return password.equals(storedPassword);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void showConnectionRequestDialog(WebSocket conn) {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/diplomremoteaccess/AccessRequest.fxml"));
                Parent root = loader.load();

                AccessRequestController controller = loader.getController();
                controller.setConnection(conn);

                Stage stage = new Stage();
                stage.setTitle("Запрос на подключение");
                stage.setScene(new Scene(root));
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    //------------------------Отправка и получение файлов---------------------------------
    private void saveFile(String fileName, String fileContent) {
        try {
            byte[] fileData = Base64.getDecoder().decode(fileContent);
            File file = new File(fileName);
            Files.write(file.toPath(), fileData);
            System.out.println("Сохранённый файл: " + fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendFile(WebSocket conn, String fileName) {
        File file = new File(fileName);
        if (file.exists()) {
            try {
                byte[] fileContent = Files.readAllBytes(file.toPath());
                String base64File = Base64.getEncoder().encodeToString(fileContent);
                conn.send("FILE " + file.getName() + " " + base64File);
                System.out.println("Отправленный файл: " + fileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Запрошенный файл не найден: " + fileName);
        }
    }

    private void sendFileList(WebSocket conn) {
        File currentDir = new File(".");
        File[] files = currentDir.listFiles();
        if (files != null) {
            StringBuilder fileList = new StringBuilder();
            for (File file : files) {
                fileList.append(file.getName()).append(";");
            }
            conn.send("FILE_LIST " + fileList.toString());
        }
    }
    //---------------------------------------------------------------------------------------

    private static String loadProperty(String key) {
        Properties properties = new Properties();
        try (FileInputStream in = new FileInputStream("computer_data.properties")) {
            properties.load(in);
            return properties.getProperty(key);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    public static void main(String[] args) throws Exception {
        String host = loadProperty("computerPublicIP");
//        if (host == null || host.isEmpty()) {
//            host = getComputerIP(); // Использовать локальный IP, если публичный IP не доступен
//        }

        int port = 8888;
        Server server = new Server(new InetSocketAddress(host, port));
        server.start();
        System.out.println("Сервер запущен: " + host + ":" + port);

        // File transfer server
        Server fileTransferServer = new Server(new InetSocketAddress(host, 8888));
        fileTransferServer.start();
        System.out.println("Сервер передачи файлов запущен на: " + host + ":8888");
    }

}

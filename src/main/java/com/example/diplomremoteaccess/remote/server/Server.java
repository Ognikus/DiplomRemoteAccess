package com.example.diplomremoteaccess.remote.server;

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
        System.out.println("Closed connection: " + conn.getRemoteSocketAddress() + " with exit code " + code + " additional info: " + reason);
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
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("Server started successfully");
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

    private void receiveFile(String fileName, String fileContent) {
        try {
            byte[] fileData = Base64.getDecoder().decode(fileContent);
            Files.write(new File("received_" + fileName).toPath(), fileData);
            System.out.println("File received: " + fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        JFrame dialog = new JFrame("Connection Request");
        dialog.setSize(300, 200);
        dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout());

        JLabel label = new JLabel("Connection request from: " + conn.getRemoteSocketAddress().getAddress());
        dialog.add(label, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        JButton acceptButton = new JButton("Принять");
        JButton declineButton = new JButton("Отклонить");

        acceptButton.addActionListener(e -> {
            dialog.dispose();
            conn.send("PASSWORD_OK");
            startScreenCapture(conn);
        });

        declineButton.addActionListener(e -> {
            dialog.dispose();
            conn.send("PASSWORD_FAIL");
            conn.close();
        });

        buttonPanel.add(acceptButton);
        buttonPanel.add(declineButton);

        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    private String loadProperty(String key) {
        Properties properties = new Properties();
        try (FileInputStream in = new FileInputStream(PROPERTIES_FILE)) {
            properties.load(in);
            return properties.getProperty(key);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) {
        InetSocketAddress address = new InetSocketAddress("0.0.0.0", 8887); // Bind to all available network interfaces
        Server server = new Server(address);
        server.start();
        System.out.println("Server started on: " + address);
    }
}

package com.example.diplomremoteaccess.remote.server;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Base64;
import java.util.Properties;

public class Server extends WebSocketServer {

    private static final String PROPERTIES_FILE = "computer_data.properties";
    private static final String PASSWORD_KEY = "oneTimePassword";
    private Robot robot;
    private Rectangle screenRect;

    public Server(InetSocketAddress address) throws AWTException {
        super(address);
        robot = new Robot();
        screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("New connection from " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Closed connection to " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        if (message.startsWith("PASSWORD")) {
            String password = message.split(" ")[1];
            if (validatePassword(password)) {
                conn.send("PASSWORD_OK");
                startScreenSharing(conn);
            } else {
                conn.send("PASSWORD_FAIL");
            }
        } else if (message.startsWith("MOUSE_MOVE")) {
            String[] coords = message.split(" ");
            int x = Integer.parseInt(coords[1]);
            int y = Integer.parseInt(coords[2]);
            robot.mouseMove(x, y);
        } else if (message.startsWith("MOUSE_CLICK")) {
            int button = Integer.parseInt(message.split(" ")[1]);
            int mask = 0;
            if (button == 1) mask = InputEvent.BUTTON1_DOWN_MASK;
            if (button == 2) mask = InputEvent.BUTTON2_DOWN_MASK;
            if (button == 3) mask = InputEvent.BUTTON3_DOWN_MASK;
            robot.mousePress(mask);
            robot.mouseRelease(mask);
        } else if (message.startsWith("KEY_PRESS")) {
            int keyCode = Integer.parseInt(message.split(" ")[1]);
            robot.keyPress(keyCode);
        } else if (message.startsWith("KEY_RELEASE")) {
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
        System.out.println("Server started!");
    }

    private void startScreenSharing(WebSocket conn) {
        new Thread(() -> {
            while (conn.isOpen()) {
                try {
                    BufferedImage screenCapture = robot.createScreenCapture(screenRect);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(screenCapture, "jpg", baos);
                    byte[] bytes = baos.toByteArray();
                    String base64Image = Base64.getEncoder().encodeToString(bytes);
                    conn.send(base64Image);
                    Thread.sleep(60); // Adjust frame rate here
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
            return storedPassword != null && storedPassword.equals(password);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void main(String[] args) {
        try {
            String computerIP = loadProperty("computerIP");
            if (computerIP == null || computerIP.trim().isEmpty()) {
                System.err.println("No valid IP address found in properties file.");
                return;
            }
            InetSocketAddress address = new InetSocketAddress(computerIP, 8887);
            Server server = new Server(address);
            server.start();
            System.out.println("Server started on: " + computerIP + ":8887");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String loadProperty(String key) {
        Properties properties = new Properties();
        try (FileInputStream in = new FileInputStream(PROPERTIES_FILE)) {
            properties.load(in);
            return properties.getProperty(key);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}

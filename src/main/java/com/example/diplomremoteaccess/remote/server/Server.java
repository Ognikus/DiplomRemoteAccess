package com.example.diplomremoteaccess.remote.server;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Base64;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Server extends WebSocketServer {

    private static final String PROPERTIES_FILE = "computer_data.properties";
    private static final String PASSWORD_KEY = "oneTimePassword";

    private Robot robot;
    private Rectangle screenRect;
    private final Set<WebSocket> connections = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public Server(InetSocketAddress address) {
        super(address);
        try {
            robot = new Robot();
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            screenRect = new Rectangle(screenSize);
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        connections.add(conn);
        System.out.println("New connection: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        connections.remove(conn);
        System.out.println("Closed connection: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        if (message.startsWith("PASSWORD ")) {
            String password = message.substring(9);
            if (validatePassword(password)) {
                conn.send("PASSWORD_OK");
            } else {
                conn.send("PASSWORD_FAIL");
            }
        } else if (message.startsWith("MOUSE_MOVE")) {
            String[] parts = message.split(" ");
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            robot.mouseMove(x, y);
        } else if (message.startsWith("MOUSE_CLICK")) {
            String[] parts = message.split(" ");
            int button = Integer.parseInt(parts[1]);
            int mask = getMouseButtonMask(button);
            if (mask != 0) {
                robot.mousePress(mask);
                robot.mouseRelease(mask);
            }
        } else if (message.startsWith("KEY_PRESS")) {
            String[] parts = message.split(" ");
            int keyCode = Integer.parseInt(parts[1]);
            robot.keyPress(keyCode);
        } else if (message.startsWith("KEY_RELEASE")) {
            String[] parts = message.split(" ");
            int keyCode = Integer.parseInt(parts[1]);
            robot.keyRelease(keyCode);
        } else {
            try {
                byte[] imageBytes = Base64.getDecoder().decode(message);
                ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
                BufferedImage image = ImageIO.read(bais);
                // обработка изображения, если необходимо
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private int getMouseButtonMask(int button) {
        switch (button) {
            case 1:
                return InputEvent.BUTTON1_DOWN_MASK;
            case 2:
                return InputEvent.BUTTON2_DOWN_MASK;
            case 3:
                return InputEvent.BUTTON3_DOWN_MASK;
            default:
                return 0;
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("Server started!");

        new Thread(() -> {
            while (true) {
                try {
                    BufferedImage screenCapture = robot.createScreenCapture(screenRect);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(screenCapture, "jpg", baos);
                    byte[] bytes = baos.toByteArray();
                    String base64Image = Base64.getEncoder().encodeToString(bytes);

                    for (WebSocket conn : connections) {
                        conn.send(base64Image);
                    }

                    Thread.sleep(30); // Adjust frame rate here
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

    public static void main(String[] args) {
        String host = loadProperty("computerIP");
        int port = 8887;
        if (host == null || host.trim().isEmpty()) {
            System.err.println("IP address not found in properties file.");
            return;
        }
        Server server = new Server(new InetSocketAddress(host, port));
        server.start();
        System.out.println("Server started on IP: " + host + " and port: " + port);
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

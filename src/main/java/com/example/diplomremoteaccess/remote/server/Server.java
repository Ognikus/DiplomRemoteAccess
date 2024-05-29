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
import java.net.InetSocketAddress;
import java.util.Base64;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Server extends WebSocketServer {

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
        if (message.startsWith("MOUSE_MOVE")) {
            String[] parts = message.split(" ");
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            robot.mouseMove(x, y);
        } else if (message.startsWith("MOUSE_CLICK")) {
            String[] parts = message.split(" ");
            int button = Integer.parseInt(parts[1]);
            robot.mousePress(InputEvent.getMaskForButton(button));
            robot.mouseRelease(InputEvent.getMaskForButton(button));
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

    public static void main(String[] args) {
        String host = "localhost";
        int port = 8887;
        Server server = new Server(new InetSocketAddress(host, port));
        server.start();
        System.out.println("Server started on port: " + port);
    }
}




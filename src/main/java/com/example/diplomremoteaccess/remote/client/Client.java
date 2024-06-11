package com.example.diplomremoteaccess.remote.client;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Base64;

public class Client extends WebSocketClient {

    private JFrame frame;
    private JLabel imageLabel;
    private String password;

    public Client(URI serverUri, String password) {
        super(serverUri);
        this.password = password;

        frame = new JFrame("Remote Desktop");
        imageLabel = new JLabel();
        frame.add(imageLabel);
        frame.setSize(1920, 1080);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setVisible(true);

        imageLabel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                send("MOUSE_MOVE " + e.getX() + " " + e.getY());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                send("MOUSE_MOVE " + e.getX() + " " + e.getY());
            }
        });

        imageLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                send("MOUSE_CLICK " + e.getButton());
            }
        });

        frame.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                send("KEY_PRESS " + e.getKeyCode());
            }

            @Override
            public void keyReleased(KeyEvent e) {
                send("KEY_RELEASE " + e.getKeyCode());
            }
        });
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("Connected to server");
        // Send the password immediately after connection
        send("PASSWORD " + password);
    }

    @Override
    public void onMessage(String message) {
        if (message.equals("PASSWORD_OK")) {
            System.out.println("Password accepted, starting remote desktop");
        } else if (message.equals("PASSWORD_FAIL")) {
            System.out.println("Password rejected, closing connection");
            close();
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
        System.out.println("Disconnected from server: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
    }

    private void receiveFile(String fileName) {
        try {
            byte[] fileContent = Base64.getDecoder().decode(fileName);
            FileOutputStream fos = new FileOutputStream(new File("received_" + fileName));
            fos.write(fileContent);
            fos.close();
            System.out.println("File received: " + fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Usage: java Client <server_ip> <password>");
            return;
        }
        String serverIP = args[0];
        String password = args[1];
        String serverUri = "ws://" + serverIP + ":8887";
        Client client = new Client(new URI(serverUri), password);
        client.connect();
    }
}
package com.example.diplomremoteaccess.remote.client;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.Base64;

public class Client extends WebSocketClient {

    private JFrame frame;
    private JLabel imageLabel;

    public Client(URI serverUri) {
        super(serverUri);
        frame = new JFrame("Remote Desktop");
        imageLabel = new JLabel();
        frame.add(imageLabel);
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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
            public void mouseClicked(MouseEvent e) {
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
    }

    @Override
    public void onMessage(String message) {
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

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Disconnected from server");
    }

    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
    }

    public static void main(String[] args) throws Exception {
        String serverUri = "ws://localhost:8887";
        Client client = new Client(new URI(serverUri));
        client.connect();
    }
}



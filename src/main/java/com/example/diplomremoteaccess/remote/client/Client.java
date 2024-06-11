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
    private String password;

    public Client(URI serverUri, String password) {
        super(serverUri);
        this.password = password;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("Connected to server");
        send("PASSWORD " + password);
    }

    @Override
    public void onMessage(String message) {
        if (message.equals("PASSWORD_OK")) {
            SwingUtilities.invokeLater(() -> {
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
            });
        } else if (message.equals("PASSWORD_FAIL")) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(null, "Invalid Password. Please try again.");
                close();
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

    public static void main(String[] args) throws Exception {
        String serverIP = "YOUR_SERVER_IP_HERE"; // Replace with actual IP
        String password = "YOUR_PASSWORD_HERE"; // Replace with actual password
        String serverUri = "ws://" + serverIP + ":8887";
        Client client = new Client(new URI(serverUri), password);
        client.connect();
    }
}

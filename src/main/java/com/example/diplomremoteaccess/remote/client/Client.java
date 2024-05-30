package com.example.diplomremoteaccess.remote.client;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import javax.imageio.ImageIO;
import javax.swing.*;
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
            Platform.runLater(() -> {
                frame = new JFrame("Удаленный рабочий стол");
                imageLabel = new JLabel();
                frame.add(imageLabel);
                frame.setSize(800, 600);
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.setVisible(true);
            });
        } else if (message.equals("PASSWORD_FAIL")) {
            Platform.runLater(() -> {
                showErrorAlert("Неверный пароль", "Введенный пароль неверен. Пожалуйста, попробуйте снова.");
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
        System.out.println("Отключен от сервера");
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

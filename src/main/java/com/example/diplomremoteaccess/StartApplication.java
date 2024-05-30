package com.example.diplomremoteaccess;

import com.example.diplomremoteaccess.remote.server.Server;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;

public class StartApplication extends Application {
    private static final String PROPERTIES_FILE = "computer_data.properties";
    private static final String IP_KEY = "computerIP";

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Загрузите файл FXML и установите сцену
        FXMLLoader loader = new FXMLLoader(getClass().getResource("StartPage.fxml"));
        Scene scene = new Scene(loader.load());
        primaryStage.setScene(scene);
        primaryStage.setTitle("Удаленный доступ");
        primaryStage.show();

        // Загрузите IP-адрес из файла свойств
        String computerIP = loadProperty(IP_KEY);
        if (computerIP == null || computerIP.trim().isEmpty()) {
            throw new RuntimeException("IP address not found in properties file.");
        }

        // Старт сервера
        startServer(computerIP);
    }

    private void startServer(String host) {
        new Thread(() -> {
            try {
                Server server = new Server(new InetSocketAddress(host, 8887));
                server.start();
                System.out.println("Сервер запущен по IP-адресу: " + host);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
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
        launch(args);
    }
}

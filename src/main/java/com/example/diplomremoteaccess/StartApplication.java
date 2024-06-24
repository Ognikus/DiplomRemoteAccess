package com.example.diplomremoteaccess;

import com.example.diplomremoteaccess.remote.server.Server;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.InetSocketAddress;

public class StartApplication extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("StartPage.fxml"));
        primaryStage.setTitle("Удалённый доступ");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();

        // Start the server
        new Thread(() -> {
            try {
                InetSocketAddress address = new InetSocketAddress("26.179.42.51", 8888); // Привязка ко всем доступным сетевым интерфейсам
                Server server = new Server(address);
                server.start();
                System.out.println("cервер запущен на: " + address);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

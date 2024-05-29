package com.example.diplomremoteaccess;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class StartApplication extends Application {
    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(StartApplication.class.getResource("StartPage.fxml"));
        Parent root = fxmlLoader.load();

        // Создание сцены и подключение CSS файла
        Scene scene = new Scene(root, 1200, 800);

        // Установка заголовка окна
        primaryStage.setTitle("Удалённый доступ главная");

        // Запрет изменения размера окна
        primaryStage.setResizable(false);

        // Установка сцены и отображение окна
        primaryStage.setScene(scene);
        primaryStage.show();

    }

    public static void main(String[] args) {
        launch(args);
    }
}

package org.example.messengermrsocks;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("/auth-activity.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 700, 500);
        stage.setMinHeight(500);
        stage.setMinWidth(700);
        stage.setScene(scene);
        stage.show();

//        stage.setTitle("Авторизация");
//        stage.setScene(new Scene(fxmlLoader.load(), 700, 500));
//        stage.setMinWidth(500);
//        stage.setMinHeight(700);
//        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
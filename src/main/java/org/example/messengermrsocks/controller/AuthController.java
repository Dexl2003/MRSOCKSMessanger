package org.example.messengermrsocks.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import java.io.IOException;

public class AuthController {
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;

    @FXML private Hyperlink forgotPasswordLink;
    @FXML private Button signInButton;

    private static final String VALID_EMAIL = "1";
    private static final String VALID_PASSWORD = "1";

    @FXML
    private void initialize() {
        // Обработчик кнопки входа
        signInButton.setOnAction(event -> handleSignIn());

        // Обработчик ссылки "Forgot password?"
        forgotPasswordLink.setOnAction(event -> handleForgotPassword());

        // Обработчик нажатия Enter в полях ввода
        emailField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                passwordField.requestFocus();
            }
        });

        passwordField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handleSignIn();
            }
        });
    }

    private void handleSignIn() {
        String email = emailField.getText();
        String password = passwordField.getText();

        if (VALID_EMAIL.equals(email) && VALID_PASSWORD.equals(password)) {
            // Открыть главное окно
            openMainActivity();
            // Закрыть окно авторизации
            signInButton.getScene().getWindow().hide();
        } else {
            // Показать ошибку
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка авторизации");
            alert.setHeaderText(null);
            alert.setContentText("Неверный email или пароль!");
            alert.showAndWait();
        }
    }

    private void handleForgotPassword() {
        // Реализация восстановления пароля
        System.out.println("Восстановление пароля");
    }

    private void openMainActivity() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main-activity.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Главное окно");
            stage.setMinWidth(700);
            stage.setMinHeight(500);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

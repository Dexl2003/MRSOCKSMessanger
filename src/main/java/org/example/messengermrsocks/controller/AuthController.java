package org.example.messengermrsocks.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;

public class AuthController {
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;

    @FXML private Hyperlink forgotPasswordLink;
    @FXML private Button signInButton;

    @FXML
    private void initialize() {
        // Обработчик кнопки входа
        signInButton.setOnAction(event -> handleSignIn());

        // Обработчик ссылки "Forgot password?"
        forgotPasswordLink.setOnAction(event -> handleForgotPassword());
    }

    private void handleSignIn() {
        String email = emailField.getText();
        String password = passwordField.getText();

        // Реализация логики авторизации
        System.out.println("Попытка входа: " + email);

    }

    private void handleForgotPassword() {
        // Реализация восстановления пароля
        System.out.println("Восстановление пароля");
    }
}

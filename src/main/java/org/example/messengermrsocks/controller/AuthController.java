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
import org.example.messengermrsocks.Networks.AuthManager;

public class AuthController {
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button signInButton;
    @FXML private Button registerButton;
    @FXML private Label errorLabel;

    private AuthManager authManager;

    @FXML
    private void initialize() {
        authManager = new AuthManager();
        setupValidation();
        
        // Обработчик кнопки входа
        signInButton.setOnAction(event -> handleSignIn());
        
        // Обработчик кнопки регистрации
        registerButton.setOnAction(event -> handleRegister());

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

    private void setupValidation() {
        // Валидация полей ввода
        emailField.textProperty().addListener((obs, oldVal, newVal) -> {
            validateFields();
        });

        passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
            validateFields();
        });
    }

    private void validateFields() {
        boolean isValid = !emailField.getText().trim().isEmpty() && 
                         !passwordField.getText().trim().isEmpty();
        signInButton.setDisable(!isValid);
        registerButton.setDisable(!isValid);
    }

    @FXML
    private void handleSignIn() {
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Пожалуйста, заполните все поля");
            return;
        }

        signInButton.setDisable(true);
        registerButton.setDisable(true);

        // Запускаем аутентификацию в отдельном потоке
        new Thread(() -> {
            boolean success = authManager.login(email, password);
            
            javafx.application.Platform.runLater(() -> {
                if (success) {
                    openMainWindow();
                } else {
                    showError("Неверный email или пароль");
                    signInButton.setDisable(false);
                    registerButton.setDisable(false);
                }
            });
        }).start();
    }

    @FXML
    private void handleRegister() {
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Пожалуйста, заполните все поля");
            return;
        }

        signInButton.setDisable(true);
        registerButton.setDisable(true);

        // Запускаем регистрацию в отдельном потоке
        new Thread(() -> {
            boolean success = authManager.register(email, password);
            
            javafx.application.Platform.runLater(() -> {
                if (success) {
                    showError("Регистрация успешна. Теперь вы можете войти.");
                } else {
                    showError("Ошибка при регистрации. Возможно, пользователь уже существует.");
                }
                signInButton.setDisable(false);
                registerButton.setDisable(false);
            });
        }).start();
    }

    @FXML
    private void handleForgotPassword() {
        errorLabel.setText("Восстановление пароля пока не реализовано.");
        errorLabel.setVisible(true);
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void openMainWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main-activity.fxml"));
            Parent root = loader.load();
            
            MainController mainController = loader.getController();
            mainController.setAuthManager(authManager);
            
            Stage stage = (Stage) emailField.getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/styles.css").toExternalForm());
            
            stage.setScene(scene);
            stage.setTitle("MRSOCKS Messenger");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Ошибка при открытии главного окна");
        }
    }
}

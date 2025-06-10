package org.example.messengermrsocks.Networks;

import org.example.messengermrsocks.model.intarfaces.AuthProvider;
import org.example.messengermrsocks.model.Peoples.User;
import org.example.messengermrsocks.model.AuthResponse;
import org.example.messengermrsocks.model.AuthError;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class AuthManager implements AuthProvider {
    private static final String SERVER_URL = "http://localhost:8080"; // Измените на ваш сервер
    private static final String AUTH_ENDPOINT = "/api/auth";
    private User currentUser;
    private String authToken;
    private String lastError;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuthManager() {
        this.currentUser = null;
        this.authToken = null;
        this.lastError = null;
    }

    public boolean login(String username, String password) {
        try {
            // Создаем JSON для запроса
            String jsonRequest = String.format(
                "{\"username\":\"%s\",\"password\":\"%s\"}",
                username, password
            );

            // Создаем URL и открываем соединение
            URL url = new URL(SERVER_URL + AUTH_ENDPOINT + "/login");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            
            // Настраиваем запрос
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);
            conn.setDoInput(true);

            // Отправляем данные
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonRequest.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // Получаем ответ
            int responseCode = conn.getResponseCode();
            String responseJson = readResponse(conn);
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                if (responseJson.isEmpty()) {
                    lastError = "Пустой ответ от сервера";
                    return false;
                }

                try {
                    // Парсим ответ
                    AuthResponse authResponse = objectMapper.readValue(responseJson, AuthResponse.class);
                    
                    // Проверяем, что все необходимые поля присутствуют
                    if (authResponse.getToken() == null || authResponse.getUser() == null) {
                        lastError = "Неверный формат ответа от сервера";
                        return false;
                    }
                    
                    // Создаем пользователя
                    this.currentUser = new User(authResponse.getUser().getUsername());
                    this.currentUser.setStatus(authResponse.getUser().getStatus());
                    this.authToken = authResponse.getToken();
                    this.lastError = null;
                    
                    return true;
                } catch (Exception e) {
                    lastError = "Ошибка при обработке ответа сервера";
                    System.err.println("Error parsing response: " + e.getMessage());
                    System.err.println("Response content: " + responseJson);
                    return false;
                }
            } else {
                // Обработка ошибок
                try {
                    AuthError authError = objectMapper.readValue(responseJson, AuthError.class);
                    lastError = authError.getError();
                } catch (Exception e) {
                    lastError = "Ошибка сервера: " + responseCode;
                }
                return false;
            }
        } catch (Exception e) {
            lastError = "Ошибка соединения с сервером";
            e.printStackTrace();
            return false;
        }
    }

    private String readResponse(HttpURLConnection conn) throws IOException {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                    conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream(),
                    "utf-8"))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            return response.toString();
        }
    }

    public String getLastError() {
        return lastError;
    }

    public boolean register(String username, String password) {
        try {
            String jsonRequest = String.format(
                "{\"username\":\"%s\",\"password\":\"%s\"}",
                username, password
            );

            URL url = new URL(SERVER_URL + AUTH_ENDPOINT + "/register");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);
            conn.setDoInput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonRequest.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            String responseJson = readResponse(conn);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                lastError = null;
                return true;
            } else {
                try {
                    AuthError authError = objectMapper.readValue(responseJson, AuthError.class);
                    lastError = authError.getError();
                } catch (Exception e) {
                    lastError = "Ошибка сервера: " + responseCode;
                }
                return false;
            }
        } catch (Exception e) {
            lastError = "Ошибка соединения с сервером";
            e.printStackTrace();
            return false;
        }
    }

    private String hashPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(password.getBytes());
        return Base64.getEncoder().encodeToString(hash);
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public String getAuthToken() {
        return authToken;
    }

    public boolean isAuthenticated() {
        return authToken != null && currentUser != null;
    }

    public void logout() {
        this.currentUser = null;
        this.authToken = null;
        this.lastError = null;
    }
}

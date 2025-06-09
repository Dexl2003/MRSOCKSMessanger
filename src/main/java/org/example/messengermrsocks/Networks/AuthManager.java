package org.example.messengermrsocks.Networks;

import org.example.messengermrsocks.model.intarfaces.AuthProvider;
import org.example.messengermrsocks.model.Peoples.User;

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

    public AuthManager() {
        this.currentUser = null;
        this.authToken = null;
    }

    public boolean login(String username, String password) {
        // Временная тестовая учётная запись
        if ("1".equals(username) && "1".equals(password)) {
            this.currentUser = new User();
            this.currentUser.setName(username);
            this.authToken = "test-token";
            return true;
        }
        try {
            String hashedPassword = hashPassword(password);
            String jsonRequest = String.format(
                "{\"username\":\"%s\",\"password\":\"%s\"}",
                username, hashedPassword
            );

            URL url = new URL(SERVER_URL + AUTH_ENDPOINT + "/login");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonRequest.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    // В реальном приложении здесь будет парсинг JSON и получение токена
                    this.authToken = response.toString();
                    this.currentUser = new User();
                    this.currentUser.setName(username);
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean register(String username, String password) {
        try {
            String hashedPassword = hashPassword(password);
            String jsonRequest = String.format(
                "{\"username\":\"%s\",\"password\":\"%s\"}",
                username, hashedPassword
            );

            URL url = new URL(SERVER_URL + AUTH_ENDPOINT + "/register");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonRequest.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            return responseCode == HttpURLConnection.HTTP_OK;
        } catch (Exception e) {
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
    }
}

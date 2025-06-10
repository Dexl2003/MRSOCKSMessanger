package org.example.messengermrsocks.Networks;

import org.example.messengermrsocks.model.intarfaces.AuthProvider;
import org.example.messengermrsocks.model.Peoples.User;
import org.example.messengermrsocks.model.AuthResponse;
import org.example.messengermrsocks.model.AuthError;
import org.example.messengermrsocks.model.UserSearchResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.ArrayList;

public class AuthManager implements AuthProvider {
    private static final String SERVER_URL = "http://localhost:8080"; // Измените на ваш сервер
    private static final String AUTH_ENDPOINT = "/api/auth";
    private static final String USERS_ENDPOINT = "/api/users";
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
        try (InputStream inputStream = conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream()) {
            if (inputStream == null) {
                return "";
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                return response.toString();
            }
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

    public List<User> searchUsers(String query) {
        if (!isAuthenticated()) {
            lastError = "Требуется авторизация";
            return new ArrayList<>();
        }

        try {
            URL url = new URL(SERVER_URL + USERS_ENDPOINT + "/search?query=" + URLEncoder.encode(query, "UTF-8"));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + authToken);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            String responseJson = readResponse(conn);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                try {
                    UserSearchResponse searchResponse = objectMapper.readValue(responseJson, UserSearchResponse.class);
                    List<User> users = new ArrayList<>();
                    
                    if (searchResponse != null && searchResponse.getUsers() != null) {
                        for (UserSearchResponse.UserInfo userInfo : searchResponse.getUsers()) {
                            if (userInfo != null && userInfo.getUsername() != null) {
                                User user = new User(userInfo.getUsername());
                                user.setStatus(userInfo.getStatus());
                                user.setIp(userInfo.getIp());
                                user.setP2pPort(userInfo.getP2pPort());
                                users.add(user);
                            }
                        }
                    }
                    
                    lastError = null;
                    return users;
                } catch (Exception e) {
                    lastError = "Ошибка при обработке ответа сервера: " + e.getMessage();
                    System.err.println("Error parsing response: " + e.getMessage());
                    return new ArrayList<>();
                }
            } else {
                try {
                    AuthError authError = objectMapper.readValue(responseJson, AuthError.class);
                    lastError = authError.getError();
                } catch (Exception e) {
                    lastError = "Ошибка сервера: " + responseCode;
                }
                return new ArrayList<>();
            }
        } catch (Exception e) {
            lastError = "Ошибка соединения с сервером: " + e.getMessage();
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public void logoutFromApi() {
        if (authToken == null) return;
        try {
            URL url = new URL(SERVER_URL + AUTH_ENDPOINT + "/logout");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + authToken);
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.getResponseCode(); // Запрос отправлен, ответ неважен
        } catch (Exception e) {
            System.err.println("Ошибка при logout: " + e.getMessage());
        }
    }
}

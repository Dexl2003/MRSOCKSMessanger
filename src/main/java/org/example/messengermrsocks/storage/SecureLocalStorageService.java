package org.example.messengermrsocks.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import org.example.messengermrsocks.model.Messages.MessengerData;
import org.example.messengermrsocks.model.Peoples.User;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SecureLocalStorageService {
    private static final String STORAGE_DIR = "user_data";
    private static final ObjectMapper objectMapper = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    static {
        // Создаем директорию для хранения данных, если она не существует
        try {
            Files.createDirectories(Paths.get(STORAGE_DIR));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static <T> void saveData(T data) throws IOException {
        if (data instanceof MessengerData) {
            MessengerData messengerData = (MessengerData) data;
            String fileName = messengerData.getStorageFileName();
            File file = new File(STORAGE_DIR, fileName);
            objectMapper.writeValue(file, data);
        } else {
            throw new IllegalArgumentException("Unsupported data type for storage");
        }
    }

    public static <T> T loadData(Class<T> type, User currentUser) throws IOException {
        if (type == MessengerData.class) {
            String fileName = "messenger_data_" + currentUser.getUsername() + ".json";
            File file = new File(STORAGE_DIR, fileName);
            
            if (file.exists()) {
                try {
                    MessengerData data = objectMapper.readValue(file, MessengerData.class);
                    data.setCurrentUser(currentUser);
                    return type.cast(data);
                } catch (Exception e) {
                    System.err.println("Error loading data: " + e.getMessage());
                    // Если произошла ошибка при загрузке, создаем новые данные
                    MessengerData newData = new MessengerData(currentUser);
                    return type.cast(newData);
                }
            } else {
                MessengerData newData = new MessengerData(currentUser);
                return type.cast(newData);
            }
        }
        return null;
    }

    public static void deleteData(String username) throws IOException {
        String fileName = "messenger_data_" + username + ".json";
        File file = new File(STORAGE_DIR, fileName);
        if (file.exists()) {
            Files.delete(file.toPath());
        }
    }
} 
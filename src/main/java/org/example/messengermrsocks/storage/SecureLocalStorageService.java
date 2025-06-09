package org.example.messengermrsocks.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Key;
import java.util.Base64;

public class SecureLocalStorageService {
    private static final String ALGORITHM = "AES";
    // В реальном приложении ключ должен храниться безопасно!
    private static final String SECRET_KEY = "MRSOCKS123456789"; // 16 символов для AES-128
    private static final String STORAGE_DIR = System.getProperty("user.home") + File.separator + ".mrsocks";
    private static final String STORAGE_FILE = STORAGE_DIR + File.separator + "data.enc";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static <T> void saveData(T data) throws Exception {
        File dir = new File(STORAGE_DIR);
        if (!dir.exists()) dir.mkdirs();
        byte[] jsonBytes = objectMapper.writeValueAsBytes(data);
        byte[] encrypted = encrypt(jsonBytes);
        Files.write(Paths.get(STORAGE_FILE), encrypted);
    }

    public static <T> T loadData(Class<T> type) throws Exception {
        File file = new File(STORAGE_FILE);
        if (!file.exists()) return null;
        byte[] encrypted = Files.readAllBytes(Paths.get(STORAGE_FILE));
        byte[] decrypted = decrypt(encrypted);
        return objectMapper.readValue(decrypted, type);
    }

    private static byte[] encrypt(byte[] data) throws Exception {
        Key key = new SecretKeySpec(SECRET_KEY.getBytes(), ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(data);
    }

    private static byte[] decrypt(byte[] encrypted) throws Exception {
        Key key = new SecretKeySpec(SECRET_KEY.getBytes(), ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(encrypted);
    }
} 
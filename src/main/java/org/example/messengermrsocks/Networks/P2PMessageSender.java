package org.example.messengermrsocks.Networks;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class P2PMessageSender {
    private static final int MAX_FRAGMENT_SIZE = 1024;
    private final SecretKey aesKey;
    private final SecretKey hmacKey;

    public P2PMessageSender(SecretKey aesKey, SecretKey hmacKey) {
        this.aesKey = aesKey;
        this.hmacKey = hmacKey;
    }

    public List<P2PFragment> fragmentAndEncrypt(byte[] message, int messageId) throws Exception {
        System.out.println("[P2PMessageSender] Начало фрагментации и шифрования сообщения. Размер: " + message.length + " байт");
        System.out.println("[P2PMessageSender] Первые 16 байт сообщения: " + bytesToHex(message, 16));
        
        List<P2PFragment> fragments = new ArrayList<>();
        int totalFragments = (int) Math.ceil((double) message.length / MAX_FRAGMENT_SIZE);
        System.out.println("[P2PMessageSender] Будет создано " + totalFragments + " фрагментов");
        
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, aesKey);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(hmacKey);
        
        for (int i = 0; i < totalFragments; i++) {
            int start = i * MAX_FRAGMENT_SIZE;
            int end = Math.min(start + MAX_FRAGMENT_SIZE, message.length);
            byte[] fragmentData = Arrays.copyOfRange(message, start, end);
            System.out.println("[P2PMessageSender] Фрагмент " + i + ": размер " + fragmentData.length + " байт");
            
            byte[] encrypted = cipher.doFinal(fragmentData);
            System.out.println("[P2PMessageSender] Зашифрованный фрагмент " + i + ": размер " + encrypted.length + " байт");
            
            byte[] hmac = mac.doFinal(encrypted);
            fragments.add(new P2PFragment(encrypted, messageId, i, totalFragments, hmac));
        }
        return fragments;
    }

    private String bytesToHex(byte[] bytes, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(bytes.length, length); i++) {
            sb.append(String.format("%02X ", bytes[i]));
        }
        return sb.toString();
    }
} 
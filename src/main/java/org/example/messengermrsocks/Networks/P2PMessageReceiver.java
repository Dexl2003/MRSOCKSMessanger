package org.example.messengermrsocks.Networks;

import java.util.*;
import javax.crypto.Mac;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class P2PMessageReceiver {
    private final Map<Integer, List<P2PFragment>> fragmentBuffer = new HashMap<>();
    private final byte[] hmacKey;
    private final byte[] aesKey;

    public P2PMessageReceiver(byte[] hmacKey, byte[] aesKey) {
        this.hmacKey = hmacKey;
        this.aesKey = aesKey;
    }

    public synchronized void receiveFragment(P2PFragment fragment) throws Exception {
        // Проверяем подпись
        if (!verifyHmac(fragment.getData(), fragment.getHmac())) {
            System.err.println("[P2PMessageReceiver] HMAC verification failed for fragment " + fragment.getFragmentIndex() + " messageId=" + fragment.getMessageId());
            throw new SecurityException("HMAC verification failed for fragment " + fragment.getFragmentIndex());
        } else {
            System.out.println("[P2PMessageReceiver] HMAC OK для fragmentIndex=" + fragment.getFragmentIndex() + ", messageId=" + fragment.getMessageId());
        }
        fragmentBuffer.computeIfAbsent(fragment.getMessageId(), k -> new ArrayList<>()).add(fragment);
        System.out.println("[P2PMessageReceiver] Фрагмент добавлен в буфер: messageId=" + fragment.getMessageId() + ", fragmentIndex=" + fragment.getFragmentIndex());
    }

    public synchronized byte[] tryAssembleMessage(int messageId) {
        List<P2PFragment> fragments = fragmentBuffer.get(messageId);
        if (fragments == null || fragments.isEmpty()) {
            System.out.println("[P2PMessageReceiver] Нет фрагментов для messageId=" + messageId);
            return null;
        }
        int total = fragments.get(0).getTotalFragments();
        if (fragments.size() < total) {
            System.out.println("[P2PMessageReceiver] Не все фрагменты получены: получено " + fragments.size() + " из " + total);
            return null;
        }
        fragments.sort(Comparator.comparingInt(P2PFragment::getFragmentIndex));
        
        try {
            // Инициализируем шифр для дешифрования
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesKey, "AES"));
            
            // Сначала собираем зашифрованные данные
            int totalLength = fragments.stream().mapToInt(f -> f.getData().length).sum();
            System.out.println("[P2PMessageReceiver] Собираем зашифрованное сообщение из " + fragments.size() + " фрагментов, общий размер: " + totalLength + " байт");
            
            byte[] encryptedMessage = new byte[totalLength];
            int pos = 0;
            for (P2PFragment f : fragments) {
                System.out.println("[P2PMessageReceiver] Копируем зашифрованный фрагмент " + f.getFragmentIndex() + ", размер: " + f.getData().length + " байт");
                System.arraycopy(f.getData(), 0, encryptedMessage, pos, f.getData().length);
                pos += f.getData().length;
            }
            
            // Дешифруем собранное сообщение
            System.out.println("[P2PMessageReceiver] Дешифруем сообщение...");
            byte[] decryptedMessage = cipher.doFinal(encryptedMessage);
            System.out.println("[P2PMessageReceiver] Сообщение дешифровано, размер: " + decryptedMessage.length + " байт");
            
            fragmentBuffer.remove(messageId);
            return decryptedMessage;
        } catch (Exception e) {
            System.err.println("[P2PMessageReceiver] Ошибка при сборке/дешифровании сообщения: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private boolean verifyHmac(byte[] data, byte[] signature) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(hmacKey, "HmacSHA256"));
        byte[] expected = mac.doFinal(data);
        return Arrays.equals(expected, signature);
    }
} 
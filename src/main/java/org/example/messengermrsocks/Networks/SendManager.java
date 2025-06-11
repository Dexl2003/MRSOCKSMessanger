package org.example.messengermrsocks.Networks;

import org.example.messengermrsocks.model.MetaData;
import org.example.messengermrsocks.model.Messages.Message;
import org.example.messengermrsocks.model.Peoples.Contact;
import org.example.messengermrsocks.model.intarfaces.SendProvider;

import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.example.messengermrsocks.Networks.P2PInviteManager;
import org.example.messengermrsocks.Networks.P2PSession;
import org.example.messengermrsocks.Networks.P2PMessageSender;
import org.example.messengermrsocks.Networks.P2PFragment;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CompletableFuture;
import java.io.IOException;
import java.net.Socket;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Random;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SendManager implements SendProvider {
    private final P2PInviteManager p2pInviteManager;
    private final SecureRandom secureRandom;
    private static final String ALGORITHM = "AES";
    private final AtomicInteger messageCounter = new AtomicInteger(0);
    private final org.example.messengermrsocks.model.Peoples.User currentUser;

    public SendManager(P2PInviteManager p2pInviteManager) {
        this.p2pInviteManager = p2pInviteManager;
        this.secureRandom = new SecureRandom();
        this.currentUser = p2pInviteManager.getCurrentUser();
    }

    @Override
    public MetaData questStatusContact(Contact contact) {
        MetaData metaData = new MetaData();
        // Generate random G and n for key exchange
        metaData.setG(secureRandom.nextInt(1000) + 2);
        metaData.setN(secureRandom.nextInt(1000) + 2);
        return metaData;
    }

    @Override
    public String generateSelfCloseKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
            keyGen.init(256);
            SecretKey secretKey = keyGen.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String generateSelfOpenKey(int param_G, String selfCloseKey) {
        try {
            // In a real implementation, this would use ECDH
            // For now, we'll use a simple XOR with param_G
            byte[] closeKeyBytes = Base64.getDecoder().decode(selfCloseKey);
            byte[] openKeyBytes = new byte[closeKeyBytes.length];
            for (int i = 0; i < closeKeyBytes.length; i++) {
                openKeyBytes[i] = (byte) (closeKeyBytes[i] ^ param_G);
            }
            return Base64.getEncoder().encodeToString(openKeyBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void sendSelfOpenKeyToContact(String selfOpenKey) {
        // In a real implementation, this would send the key through P2P
    }

    @Override
    public String generateGeneralCloseKey(String questOpenKey, String selfCloseKey) {
        try {
            // In a real implementation, this would use ECDH
            // For now, we'll use a simple XOR
            byte[] openKeyBytes = Base64.getDecoder().decode(questOpenKey);
            byte[] closeKeyBytes = Base64.getDecoder().decode(selfCloseKey);
            byte[] generalKeyBytes = new byte[openKeyBytes.length];
            for (int i = 0; i < openKeyBytes.length; i++) {
                generalKeyBytes[i] = (byte) (openKeyBytes[i] ^ closeKeyBytes[i]);
            }
            return Base64.getEncoder().encodeToString(generalKeyBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Boolean sendMessage(String text, Contact contact) {
        try {
            // Получаем защищённую сессию для контакта
            P2PSession session = p2pInviteManager.getSessionMap().get(contact.getName());
            if (session == null) {
                return false;
            }
            P2PMessageSender sender = new P2PMessageSender(session.getAesKey(), session.getHmacKey());

            // Сериализуем сообщение
            Message message = new Message(text, 
                java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
                "/images/image.png", true);
            // Устанавливаем mainText (username отправителя)
            message.setMainText(currentUser.getUsername());
            byte[] messageBytes = serializeMessage(message);

            // Генерируем уникальный messageId
            int messageId = messageCounter.incrementAndGet();

            // Фрагментируем и шифруем
            List<P2PFragment> fragments = sender.fragmentAndEncrypt(messageBytes, messageId);

            // Получаем remotePorts для контакта
            List<Integer> ports = p2pInviteManager.getRemotePorts(contact.getName());
            if (ports == null) {
                ports = generatePorts(fragments.size());
            } else if (ports.size() < fragments.size()) {
                ports = generatePorts(fragments.size());
            } else if (ports.size() > fragments.size()) {
                ports = ports.subList(0, fragments.size());
            }
            message.setListNextPort(ports);

            // Асинхронно отправляем фрагменты
            CompletableFuture<Boolean> sendFuture = sendFragmentsAsync(fragments, contact.getIp(), ports);
            return sendFuture.join();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private List<Integer> generatePorts(int count) {
        List<Integer> ports = new ArrayList<>();
        Random random = new Random();
        int MIN_PORT = 49152;
        int MAX_PORT = 65535;
        for (int i = 0; i < count; i++) {
            int port;
            do {
                port = MIN_PORT + random.nextInt(MAX_PORT - MIN_PORT);
            } while (ports.contains(port));
            ports.add(port);
        }
        return ports;
    }

    public CompletableFuture<Boolean> sendFragmentsAsync(List<P2PFragment> fragments, String ip, List<Integer> ports) {
        List<CompletableFuture<Boolean>> sendFutures = new ArrayList<>();
        for (int i = 0; i < fragments.size(); i++) {
            final int fragmentIndex = i;
            final int port = ports.get(i);
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                try (Socket socket = new Socket(ip, port);
                     ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
                    out.writeObject(fragments.get(fragmentIndex));
                    return true;
                } catch (IOException e) {
                    return false;
                }
            });
            sendFutures.add(future);
        }
        return CompletableFuture.allOf(sendFutures.toArray(new CompletableFuture[0]))
            .thenApply(v -> sendFutures.stream().allMatch(CompletableFuture::join));
    }

    private byte[] serializeMessage(Message message) throws Exception {
        try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
             java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(baos)) {
            oos.writeObject(message);
            return baos.toByteArray();
        }
    }

    public void shutdown() {
        // Если нужно, можно добавить логику завершения
    }
}

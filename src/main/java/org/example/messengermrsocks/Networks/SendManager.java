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

public class SendManager implements SendProvider {
    private final P2PConnectionManager p2pManager;
    private final SecureRandom secureRandom;
    private static final String ALGORITHM = "AES";

    public SendManager() {
        this.p2pManager = new P2PConnectionManager();
        this.secureRandom = new SecureRandom();
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
        System.out.println("Sending open key: " + selfOpenKey);
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
            // Create message object
            Message message = new Message(text, 
                java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
                "/images/image.png", true);

            // Encrypt message using AES
            String generalCloseKey = generateGeneralCloseKey(contact.getOpenKey(), generateSelfCloseKey());
            SecretKey secretKey = new SecretKeySpec(Base64.getDecoder().decode(generalCloseKey), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedText = cipher.doFinal(text.getBytes());
            message.setText(Base64.getEncoder().encodeToString(encryptedText));

            // Send message through P2P
            return p2pManager.sendMessage(message, contact);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void shutdown() {
        p2pManager.shutdown();
    }
}

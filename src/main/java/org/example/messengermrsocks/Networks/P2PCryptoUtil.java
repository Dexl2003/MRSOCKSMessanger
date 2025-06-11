package org.example.messengermrsocks.Networks;

import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.util.Arrays;

public class P2PCryptoUtil {
    public static KeyPair generateECDHKeyPair() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(256);
        return kpg.generateKeyPair();
    }

    public static byte[] computeSharedSecret(PrivateKey privateKey, PublicKey remotePublicKey) throws Exception {
        KeyAgreement ka = KeyAgreement.getInstance("ECDH");
        ka.init(privateKey);
        ka.doPhase(remotePublicKey, true);
        return ka.generateSecret();
    }

    public static SecretKey deriveAESKey(byte[] sharedSecret) {
        // Используем первые 16 байт для AES-128
        byte[] keyBytes = Arrays.copyOf(sharedSecret, 16);
        return new SecretKeySpec(keyBytes, "AES");
    }

    public static SecretKey deriveHMACKey(byte[] sharedSecret) {
        // Используем следующие 16 байт для HMAC
        byte[] keyBytes = Arrays.copyOfRange(sharedSecret, 16, 32);
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }
} 
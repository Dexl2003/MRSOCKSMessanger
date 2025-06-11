package org.example.messengermrsocks.Networks;

import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.security.PublicKey;

public class P2PSession {
    private final SecretKey aesKey;
    private final SecretKey hmacKey;
    private final KeyPair ecdhKeyPair;
    private PublicKey remoteECDHPublicKey;

    public P2PSession(SecretKey aesKey, SecretKey hmacKey, KeyPair ecdhKeyPair) {
        this.aesKey = aesKey;
        this.hmacKey = hmacKey;
        this.ecdhKeyPair = ecdhKeyPair;
    }

    public SecretKey getAesKey() { return aesKey; }
    public SecretKey getHmacKey() { return hmacKey; }
    public KeyPair getECDHKeyPair() { return ecdhKeyPair; }
    public PublicKey getRemoteECDHPublicKey() { return remoteECDHPublicKey; }
    public void setRemoteECDHPublicKey(PublicKey remoteECDHPublicKey) { this.remoteECDHPublicKey = remoteECDHPublicKey; }
} 
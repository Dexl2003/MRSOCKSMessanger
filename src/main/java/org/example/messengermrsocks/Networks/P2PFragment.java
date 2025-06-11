package org.example.messengermrsocks.Networks;

import java.io.Serializable;

public class P2PFragment implements Serializable {
    private static final long serialVersionUID = 1L;
    private final byte[] data;
    private final int messageId;
    private final int fragmentIndex;
    private final int totalFragments;
    private final byte[] hmac;

    public P2PFragment(byte[] data, int messageId) {
        this.data = data;
        this.messageId = messageId;
        this.fragmentIndex = 0; // Будет установлено позже
        this.totalFragments = 0; // Будет установлено позже
        this.hmac = null; // Будет установлено позже
    }

    public P2PFragment(byte[] data, int messageId, int fragmentIndex, int totalFragments, byte[] hmac) {
        this.data = data;
        this.messageId = messageId;
        this.fragmentIndex = fragmentIndex;
        this.totalFragments = totalFragments;
        this.hmac = hmac;
    }

    public byte[] getData() {
        return data;
    }

    public int getMessageId() {
        return messageId;
    }

    public int getFragmentIndex() {
        return fragmentIndex;
    }

    public int getTotalFragments() {
        return totalFragments;
    }

    public byte[] getHmac() {
        return hmac;
    }
} 
package org.example.messengermrsocks.model.Messages;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class P2PInviteResponse {
    private String type = "p2p_invite_response";
    private String from;
    private String to;
    private boolean accepted;
    private long timestamp;
    private String ecdhPublicKeyBase64;
    private List<Integer> ports;

    public P2PInviteResponse() {}

    public P2PInviteResponse(String from, String to, boolean accepted, String ecdhPublicKeyBase64, List<Integer> ports) {
        this.from = from;
        this.to = to;
        this.accepted = accepted;
        this.timestamp = System.currentTimeMillis();
        this.ecdhPublicKeyBase64 = ecdhPublicKeyBase64;
        this.ports = ports;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getEcdhPublicKeyBase64() {
        return ecdhPublicKeyBase64;
    }

    public void setEcdhPublicKeyBase64(String ecdhPublicKeyBase64) {
        this.ecdhPublicKeyBase64 = ecdhPublicKeyBase64;
    }

    public List<Integer> getPorts() {
        return ports;
    }

    public void setPorts(List<Integer> ports) {
        this.ports = ports;
    }
} 
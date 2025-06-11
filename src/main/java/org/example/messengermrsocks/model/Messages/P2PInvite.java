package org.example.messengermrsocks.model.Messages;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class P2PInvite {
    private String type = "p2p_invite";
    private String from;
    private String to;
    private long timestamp;
    private String ip;
    private int port;
    private String ecdhPublicKeyBase64;
    private List<Integer> ports;

    public P2PInvite() {}

    public P2PInvite(String from, String to, String ip, int port, String ecdhPublicKeyBase64, List<Integer> ports) {
        this.from = from;
        this.to = to;
        this.timestamp = System.currentTimeMillis();
        this.ip = ip;
        this.port = port;
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

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
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
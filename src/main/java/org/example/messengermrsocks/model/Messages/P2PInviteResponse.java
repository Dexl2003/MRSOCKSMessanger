package org.example.messengermrsocks.model.Messages;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class P2PInviteResponse {
    private String type = "p2p_invite_response";
    private String from;
    private String to;
    private boolean accepted;
    private long timestamp;

    public P2PInviteResponse() {}

    public P2PInviteResponse(String from, String to, boolean accepted) {
        this.from = from;
        this.to = to;
        this.accepted = accepted;
        this.timestamp = System.currentTimeMillis();
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
} 
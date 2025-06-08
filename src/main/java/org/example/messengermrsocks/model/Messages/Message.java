package org.example.messengermrsocks.model.Messages;

import java.util.List;

public class Message {
    private String date;
    private String time;
    private String mainText;
    private String mediaDir;
    private List<Integer> listNextPort;
    private String text;
    private String avatarUrl;
    private boolean isOwn;

    public Message(String text, String time, String avatarUrl, boolean isOwn) {
        this.text = text;
        this.time = time;
        this.avatarUrl = avatarUrl;
        this.isOwn = isOwn;
    }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public boolean isOwn() { return isOwn; }
    public void setOwn(boolean own) { isOwn = own; }
}

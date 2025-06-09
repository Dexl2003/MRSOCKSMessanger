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
    private boolean isSent;
    private boolean isReceived;
    private String mediaFilePath;

    public Message() {}

    public Message(String text, String time, String avatarUrl, boolean isOwn) {
        this.text = text;
        this.time = time;
        this.avatarUrl = avatarUrl;
        this.isOwn = isOwn;
        this.isSent = false;
        this.isReceived = false;
        this.mediaFilePath = null;
    }

    public Message(String text, String time, String avatarUrl, boolean isOwn, String mediaFilePath) {
        this(text, time, avatarUrl, isOwn);
        this.mediaFilePath = mediaFilePath;
    }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public String getMainText() { return mainText; }
    public void setMainText(String mainText) { this.mainText = mainText; }
    public String getMediaDir() { return mediaDir; }
    public void setMediaDir(String mediaDir) { this.mediaDir = mediaDir; }
    public List<Integer> getListNextPort() { return listNextPort; }
    public void setListNextPort(List<Integer> listNextPort) { this.listNextPort = listNextPort; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public boolean isOwn() { return isOwn; }
    public void setOwn(boolean own) { isOwn = own; }
    public boolean isSent() { return isSent; }
    public void setSent(boolean sent) { isSent = sent; }
    public boolean isReceived() { return isReceived; }
    public void setReceived(boolean received) { isReceived = received; }
    public String getMediaFilePath() { return mediaFilePath; }
    public void setMediaFilePath(String path) { this.mediaFilePath = path; }
}

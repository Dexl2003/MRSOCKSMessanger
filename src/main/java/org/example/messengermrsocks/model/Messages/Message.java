package org.example.messengermrsocks.model.Messages;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    private String type;
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
    private String payload;

    public Message() {}

    public Message(String text, String time, String avatarUrl, boolean isOwn) {
        this.type = "text";
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
        this.type = "media";
        this.mediaFilePath = mediaFilePath;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
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
    public void setMediaFilePath(String mediaFilePath) { this.mediaFilePath = mediaFilePath; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

}

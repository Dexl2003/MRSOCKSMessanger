package org.example.messengermrsocks.model.Peoples;

import java.util.List;

public class Contact extends People {
    private int mainPort;
    private List<Integer> firstListPort;
    private String time;
    private String avatarUrl;

    public Contact(String name, String time, String avatarUrl) {
        super();
        this.setName(name);
        this.time = time;
        this.avatarUrl = avatarUrl;
    }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
}

package org.example.messengermrsocks.model.Peoples;

import java.util.List;

public class Contact extends People {
    private int mainPort;
    private List<Integer> firstListPort;
    private String time;
    private String avatarUrl;

    public Contact() { super(); }

    public Contact(String name, String time, String avatarUrl) {
        super();
        this.setName(name);
        this.time = time;
        this.avatarUrl = avatarUrl;
    }

    public int getMainPort() { return mainPort; }
    public void setMainPort(int mainPort) { this.mainPort = mainPort; }

    public List<Integer> getFirstListPort() { return firstListPort; }
    public void setFirstListPort(List<Integer> firstListPort) { this.firstListPort = firstListPort; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
}

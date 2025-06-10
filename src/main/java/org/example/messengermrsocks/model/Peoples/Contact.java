package org.example.messengermrsocks.model.Peoples;

import java.util.List;

public class Contact extends People {
    private int mainPort;
    private List<Integer> firstListPort;
    private String time;
    private String avatarUrl;
    private String openKey;
    private boolean isConnected;

    public Contact() { super(); }

    public Contact(String name, String time, String avatarUrl) {
        super();
        this.setName(name);
        this.time = time;
        this.avatarUrl = avatarUrl;
        this.isConnected = false;
    }

    public int getMainPort() { return mainPort; }
    public void setMainPort(int mainPort) { this.mainPort = mainPort; }

    public List<Integer> getFirstListPort() { return firstListPort; }
    public void setFirstListPort(List<Integer> firstListPort) { this.firstListPort = firstListPort; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getOpenKey() { return openKey; }
    public void setOpenKey(String openKey) { this.openKey = openKey; }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Contact contact = (Contact) o;
        return getName() != null && getName().equals(contact.getName());
    }

    @Override
    public int hashCode() {
        return getName() != null ? getName().hashCode() : 0;
    }
}

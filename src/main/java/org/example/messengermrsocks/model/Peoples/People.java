package org.example.messengermrsocks.model.Peoples;

public class People {
    private String ip;
    private String name;
    private boolean status;
    private String photoDir;

    public People() {}

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isStatus() { return status; }
    public void setStatus(boolean status) { this.status = status; }

    public String getPhotoDir() { return photoDir; }
    public void setPhotoDir(String photoDir) { this.photoDir = photoDir; }
}

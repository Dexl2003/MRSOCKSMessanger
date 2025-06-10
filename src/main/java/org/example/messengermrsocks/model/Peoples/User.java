package org.example.messengermrsocks.model.Peoples;

public class User {
    private String username;
    private String status;
    private String token;
    private String ip;
    private String photoDir;
    private Integer p2pPort;

    public User() {}

    public User(String username) {
        this.username = username;
        this.status = "offline";
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getPhotoDir() {
        return photoDir;
    }

    public void setPhotoDir(String photoDir) {
        this.photoDir = photoDir;
    }

    public Integer getP2pPort() {
        return p2pPort;
    }

    public void setP2pPort(Integer p2pPort) {
        this.p2pPort = p2pPort;
    }

    @Override
    public String toString() {
        return username;
    }
}

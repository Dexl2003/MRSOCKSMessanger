package org.example.messengermrsocks.model;

import com.google.gson.annotations.SerializedName;

public class AuthMessage {
    @SerializedName("type")
    private String type;
    
    @SerializedName("username")
    private String username;
    
    @SerializedName("password")
    private String password;
    
    @SerializedName("token")
    private String token;
    
    @SerializedName("success")
    private boolean success;
    
    @SerializedName("error")
    private String error;

    public AuthMessage() {
    }

    public AuthMessage(String type, String username, String password) {
        this.type = type;
        this.username = username;
        this.password = password;
    }

    // Getters and Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
} 
package org.example.messengermrsocks.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthResponse {
    @JsonProperty("token")
    private String token;

    @JsonProperty("user")
    private UserInfo user;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UserInfo getUser() {
        return user;
    }

    public void setUser(UserInfo user) {
        this.user = user;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserInfo {
        @JsonProperty("username")
        private String username;

        @JsonProperty("status")
        private String status;

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
    }
} 
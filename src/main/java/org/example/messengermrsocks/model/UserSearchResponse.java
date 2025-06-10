package org.example.messengermrsocks.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UserSearchResponse {
    @JsonProperty("users")
    private List<UserInfo> users;

    public List<UserInfo> getUsers() {
        return users;
    }

    public void setUsers(List<UserInfo> users) {
        this.users = users;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserInfo {
        @JsonProperty("username")
        private String username;

        @JsonProperty("status")
        private String status;

        @JsonProperty("ip")
        private String ip;

        @JsonProperty("p2pPort")
        private Integer p2pPort;

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

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public Integer getP2pPort() {
            return p2pPort;
        }

        public void setP2pPort(Integer p2pPort) {
            this.p2pPort = p2pPort;
        }
    }
} 
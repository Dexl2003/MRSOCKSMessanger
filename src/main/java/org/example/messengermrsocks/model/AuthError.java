package org.example.messengermrsocks.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthError {
    @JsonProperty("error")
    private String error;

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
} 
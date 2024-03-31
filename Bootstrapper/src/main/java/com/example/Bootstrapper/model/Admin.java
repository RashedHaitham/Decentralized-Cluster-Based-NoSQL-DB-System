package com.example.Bootstrapper.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.json.simple.JSONObject;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Admin {
    private String username;
    private String password;

    @SuppressWarnings("unchecked")
    public JSONObject toJson() {
        JSONObject userJson = new JSONObject();
        userJson.put("username", username);
        userJson.put("password", password);
        return userJson;
    }
}

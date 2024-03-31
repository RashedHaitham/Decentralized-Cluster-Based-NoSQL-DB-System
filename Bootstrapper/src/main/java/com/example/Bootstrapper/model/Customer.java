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
public class Customer {
    private String accountNumber;
    private String password;

    @SuppressWarnings("unchecked")
    public JSONObject toJson() {
        JSONObject userJson = new JSONObject();
        userJson.put("accountNumber", accountNumber);
        userJson.put("password", password);
        return userJson;
    }
}
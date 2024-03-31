package com.example.Bootstrapper.model;

import lombok.Getter;
import lombok.Setter;
import org.json.simple.JSONObject;

@Getter
@Setter
public class Node {
    private int nodeNumber;
    private JSONObject nodeJsonObject;
    private String nodeIP;
    private boolean isActive;
}
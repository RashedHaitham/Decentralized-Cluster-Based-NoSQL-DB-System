package com.example.Database.json;

import org.json.simple.JSONObject;

public class JsonBuilder {
    private final JSONObject jsonObject;
    private JsonBuilder(){
        jsonObject=new JSONObject();
    }

    public JsonBuilder add(String key,Object value){
        jsonObject.put(key,value);
        return this;
    }

    public JSONObject build(){
        return jsonObject;
    }

    public static JsonBuilder getBuilder(){
        return new JsonBuilder();
    }
}
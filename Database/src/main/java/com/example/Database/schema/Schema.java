package com.example.Database.schema;

import lombok.Getter;
import lombok.Setter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Collections;
import java.util.Map;

@SuppressWarnings("unchecked")

@Setter
@Getter
public class Schema {
    private String type;
    private Map<String, String> properties;
    private String[] required;

    public JSONObject toJson() {
        JSONObject jsonSchema = new JSONObject();
        jsonSchema.put("type", getType());
        JSONObject props = new JSONObject();
        props.putAll(getProperties());
        jsonSchema.put("properties", props);
        JSONArray reqArray = new JSONArray();
        Collections.addAll(reqArray, getRequired());
        jsonSchema.put("required", reqArray);
        return jsonSchema;
    }
}
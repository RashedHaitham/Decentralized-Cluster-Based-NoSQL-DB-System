package com.example.Database.schema;

import com.example.Database.schema.datatype.DataTypes;

import java.util.LinkedHashMap;
import java.util.Map;

public final class SchemaBuilder {

    private SchemaBuilder(){}

    public static Schema buildSchema() {
        Schema schema = new Schema();
        schema.setType("object");
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("accountNumber", DataTypes.LONG.getValue());
        properties.put("clientName", DataTypes.STRING.getValue());
        properties.put("balance", DataTypes.DOUBLE.getValue());
        properties.put("accountType", DataTypes.STRING.getValue());
        properties.put("status", DataTypes.BOOLEAN.getValue());
        properties.put("password", DataTypes.STRING.getValue());
        schema.setProperties(properties);
        String[] allKeys = properties.keySet().toArray(new String[0]);
        schema.setRequired(allKeys);
        return schema;
    }
}
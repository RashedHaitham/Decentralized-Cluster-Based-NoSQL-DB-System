package com.example.Database.schema.datatype;

public enum DataTypes {
    STRING("STRING"),
    LONG("LONG"),
    DOUBLE("DOUBLE"),
    BOOLEAN("BOOLEAN");

    private final String value;

    DataTypes(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
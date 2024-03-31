package com.example.Database.schema.datatype;

import com.example.Database.file.DatabaseFileOperations;
import org.json.simple.JSONObject;
import com.example.Database.file.FileService;
import java.util.Objects;

public final class DataTypeUtil {

    private DataTypeUtil(){

    }

    public static Object castToDataType(String value, String collectionName, String propertyName) {
        String dataType = getDataType(collectionName, propertyName);
        return switch (Objects.requireNonNull(dataType).toUpperCase()) {
            case "STRING" -> value;
            case "LONG" -> Long.parseLong(value);
            case "DOUBLE" -> Double.parseDouble(value);
            case "BOOLEAN" -> Boolean.parseBoolean(value);
            default -> null;
        };
    }

    public static String getDataType(String collectionName, String property) {
        JSONObject schema = FileService.readSchema(collectionName);
        if (schema != null && schema.containsKey("properties") && ((JSONObject) schema.get("properties")).containsKey(property)) {
            return (String) ((JSONObject) schema.get("properties")).get(property);
        }
        return null;
    }
}
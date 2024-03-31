package com.example.Database.query.command;

import com.example.Database.model.Database;
import com.example.Database.model.InMemoryDatabase;
import org.json.simple.JSONObject;
import com.example.Database.exceptions.*;
import com.example.Database.model.Collection;
import java.util.Optional;

public class CommandUtils {

    public static Database getDatabase(JSONObject commandJson) throws DatabaseNotFoundException {
        String databaseName = (String) commandJson.get("databaseName");
        Database database = InMemoryDatabase.getInstance().getOrCreateDatabase(databaseName);
        if (database == null) {
            throw new DatabaseNotFoundException();
        }
        return database;
    }

    public static Collection getCollection(JSONObject commandJson) throws CollectionNotFoundException {
        String collectionName = (String) commandJson.get("collectionName");
        Optional<Collection> collection = Optional.ofNullable(collectionName)
                .filter(name -> !name.trim().isEmpty())
                .map(Collection::new);
        return collection.orElseThrow(CollectionNotFoundException::new);
    }

    public static JSONObject getDocumentJson(JSONObject commandJson) throws DocumentNotFoundException {
        Optional<JSONObject> documentJson = Optional.ofNullable((JSONObject) commandJson.get("document"));
        return documentJson.orElseThrow(DocumentNotFoundException::new);
    }

    public static String getDocumentId(JSONObject commandJson) throws DocumentIdNotFoundException {
        Optional<String> documentId = Optional.ofNullable((String) commandJson.get("documentId"));
        return documentId.orElseThrow(DocumentIdNotFoundException::new);
    }

    public static String getPropertyName(JSONObject commandJson) throws PropertyNameNotFound {
        Optional<String> indexProperty = Optional.ofNullable((String) commandJson.get("propertyName"));
        return indexProperty.orElseThrow(PropertyNameNotFound::new);
    }

    public static Object getNewPropertyValue(JSONObject commandJson) throws NewPropertyValueNotFound {
        Optional<Object> indexProperty = Optional.ofNullable(commandJson.get("newPropertyValue"));
        return indexProperty.orElseThrow(NewPropertyValueNotFound::new);
    }

    public static JSONObject getSchemaJson(JSONObject commandJson) throws SchemaNotFoundException {
        Optional<JSONObject> schema = Optional.ofNullable((JSONObject) commandJson.get("schema"));
        return schema.orElseThrow(SchemaNotFoundException::new);
    }
}
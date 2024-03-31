package com.example.Database.query;

import com.example.Database.json.JsonBuilder;
import com.example.Database.model.ApiResponse;
import com.example.Database.query.command.QueryCommand;
import com.example.Database.query.command.factory.QueryCommandFactory;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.json.simple.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class QueryManager {

    private final Map<QueryType, QueryCommand> queryMap;

    @Autowired
    public QueryManager(List<QueryCommand> commandList) {
        this.queryMap = QueryCommandFactory.createQueryMap(commandList);
    }

    public ApiResponse createDatabase(String dbName, String isBroadcasted) {
        JSONObject jsonObject = JsonBuilder.getBuilder()
                .add("queryType", QueryType.CREATE_DATABASE.toString())
                .add("databaseName", dbName)
                .add("X-Broadcast", isBroadcasted)
                .build();
        return execute(jsonObject);
    }

    public ApiResponse deleteDatabase(String dbName, String isBroadcasted) {
        JSONObject jsonObject = JsonBuilder.getBuilder()
                .add("queryType", QueryType.DELETE_DATABASE.toString())
                .add("databaseName", dbName)
                .add("X-Broadcast", isBroadcasted)
                .build();
        return execute(jsonObject);
    }

    public List<String> readDatabases() {
        JSONObject jsonObject = JsonBuilder.getBuilder()
                .add("queryType", QueryType.READ_DATABASES.toString())
                .build();
        ApiResponse response = execute(jsonObject);
        List<String> databaseList = new ArrayList<>();
        if (response != null && response.getStatus() == HttpStatus.OK) {
            Collections.addAll(databaseList, response.getMessage().split(", "));
        }
        return databaseList;
    }

    public ApiResponse createCollection(String dbName, String collectionName, JSONObject schema, String isBroadcasted) {
        JSONObject jsonObject = JsonBuilder.getBuilder()
                .add("queryType", QueryType.CREATE_COLLECTION.toString())
                .add("databaseName", dbName)
                .add("collectionName", collectionName)
                .add("schema", schema)
                .add("X-Broadcast", isBroadcasted)
                .build();
        return execute(jsonObject);
    }

    public ApiResponse deleteCollection(String dbName, String collectionName, String isBroadcasted) {
        JSONObject jsonObject = JsonBuilder.getBuilder()
                .add("queryType", QueryType.DELETE_COLLECTION.toString())
                .add("databaseName", dbName)
                .add("collectionName", collectionName)
                .add("X-Broadcast", isBroadcasted)
                .build();
        return execute(jsonObject);
    }

    public List<String> readCollections(String databaseName) {
        JSONObject jsonObject = JsonBuilder.getBuilder()
                .add("queryType", QueryType.READ_COLLECTIONS.toString())
                .add("databaseName", databaseName)
                .build();
        ApiResponse response = execute(jsonObject);
        List<String> collectionList = new ArrayList<>();
        if (response != null && response.getStatus() == HttpStatus.OK) {
            Collections.addAll(collectionList, response.getMessage().split(", "));
        }
        return collectionList;
    }

    public ApiResponse createDocument(String databaseName, String collectionName, JSONObject document, String isBroadcasted) {
        JSONObject jsonObject = JsonBuilder.getBuilder()
                .add("queryType", QueryType.CREATE_DOCUMENT.toString())
                .add("databaseName", databaseName)
                .add("collectionName", collectionName)
                .add("document", document)
                .add("X-Broadcast", isBroadcasted)
                .build();
        System.out.println(isBroadcasted);
        return execute(jsonObject);
    }

    public ApiResponse deleteDocument(String databaseName, String collectionName, String documentId, String isBroadcasted) {
        JSONObject jsonObject = JsonBuilder.getBuilder()
                .add("queryType", QueryType.DELETE_DOCUMENT.toString())
                .add("databaseName", databaseName)
                .add("collectionName", collectionName)
                .add("documentId", documentId)
                .add("X-Broadcast", isBroadcasted)
                .build();
        return execute(jsonObject);
    }

    public List<JSONObject> readDocuments(String databaseName, String collectionName) {
        JSONObject jsonObject = JsonBuilder.getBuilder()
                .add("queryType", QueryType.READ_DOCUMENTS.toString())
                .add("databaseName", databaseName)
                .add("collectionName", collectionName)
                .build();
        ApiResponse response = execute(jsonObject);
        if (response.getStatus() == HttpStatus.OK) {
            JSONParser jsonParser = new JSONParser();
            try {
                JSONArray documentsArray = (JSONArray) jsonParser.parse(response.getMessage());
                List<JSONObject> documentList = new ArrayList<>();
                for (Object documentObj : documentsArray) {
                    JSONObject document = (JSONObject) documentObj;
                    documentList.add(document);
                }
                return documentList;
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return Collections.emptyList();
    }

    public ApiResponse updateDocumentProperty(String databaseName, String collectionName, String documentId, String propertyName,
                                              Object newPropertyValue, String isBroadcasted) {
        JSONObject jsonObject = JsonBuilder.getBuilder()
                .add("queryType", QueryType.UPDATE_DOCUMENT_PROPERTY.toString())
                .add("databaseName", databaseName)
                .add("collectionName", collectionName)
                .add("documentId", documentId)
                .add("propertyName", propertyName)
                .add("newPropertyValue", newPropertyValue)
                .add("X-Broadcast", isBroadcasted)
                .build();
        return execute(jsonObject);
    }


    public ApiResponse searchForProperty(String databaseName, String collectionName, String documentId, String propertyName) {
        JSONObject jsonObject = JsonBuilder.getBuilder()
                .add("queryType", QueryType.SEARCH.toString())
                .add("databaseName", databaseName)
                .add("collectionName", collectionName)
                .add("documentId", documentId)
                .add("propertyName", propertyName)
                .build();
        return execute(jsonObject);
    }

    private ApiResponse execute(JSONObject query) {
        QueryType queryType = QueryType.valueOf((String) query.get("queryType"));
        return queryMap.get(queryType).execute(query);
    }
}
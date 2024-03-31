package com.example.Database.query.command;

import com.example.Database.model.ApiResponse;
import com.example.Database.query.QueryType;
import org.json.simple.JSONObject;

public interface QueryCommand {
    QueryType getQueryType();
    ApiResponse execute(JSONObject query);
    default void broadcastOperation(JSONObject details) {
        //default implementation do nothing
    }
}
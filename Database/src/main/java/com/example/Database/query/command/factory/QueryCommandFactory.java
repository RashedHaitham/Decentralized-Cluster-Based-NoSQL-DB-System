package com.example.Database.query.command.factory;

import com.example.Database.query.command.QueryCommand;
import com.example.Database.query.QueryType;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QueryCommandFactory {

    private QueryCommandFactory() {
    }

    public static Map<QueryType, QueryCommand> createQueryMap(List<QueryCommand> commandList) {
        Map<QueryType, QueryCommand> queryMap = new HashMap<>();
        for (QueryCommand command : commandList) {
            queryMap.put(command.getQueryType(), command);
        }
        return Collections.unmodifiableMap(queryMap);
    }
}
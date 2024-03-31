package com.example.Database.query.command.collection;

import com.example.Database.model.ApiResponse;
import com.example.Database.model.Database;
import com.example.Database.query.command.CommandUtils;
import com.example.Database.query.command.QueryCommand;
import com.example.Database.query.QueryType;
import com.example.Database.services.database.CollectionService;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReadCollectionsCommand implements QueryCommand {

    private final CollectionService collectionService;

    @Autowired
    public ReadCollectionsCommand(CollectionService collectionService){
        this.collectionService = collectionService;
    }


    @Override
    public QueryType getQueryType() {
        return QueryType.READ_COLLECTIONS;
    }

    @Override
    public ApiResponse execute(JSONObject query) {
        try {
            Database database = CommandUtils.getDatabase(query);
            List<String> collections = collectionService.readCollections(database);
            return new ApiResponse(String.join(", ", collections), HttpStatus.OK);
        } catch (Exception e) {
            return new ApiResponse("Error reading collections: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
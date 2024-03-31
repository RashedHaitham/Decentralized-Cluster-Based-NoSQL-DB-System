package com.example.Database.query.command.database;

import com.example.Database.model.ApiResponse;
import com.example.Database.query.command.QueryCommand;
import com.example.Database.query.QueryType;
import com.example.Database.services.database.DatabaseService;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReadDBsCommand implements QueryCommand {

    private final DatabaseService databaseService;

    @Autowired
    public ReadDBsCommand(DatabaseService databaseService){
        this.databaseService = databaseService;
    }

    @Override
    public QueryType getQueryType() {
        return QueryType.READ_DATABASES;
    }

    @Override
    public ApiResponse execute(JSONObject query) {
        try {
            List<String> databases = databaseService.readDBs();
            if (databases.isEmpty()) {
                return new ApiResponse("", HttpStatus.NO_CONTENT);
            } else {
                return new ApiResponse(String.join(", ", databases), HttpStatus.OK);
            }
        } catch (Exception e) {
            return new ApiResponse("Error retrieving databases: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
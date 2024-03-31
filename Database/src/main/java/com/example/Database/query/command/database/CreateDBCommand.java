package com.example.Database.query.command.database;

import com.example.Database.affinity.AffinityManager;
import com.example.Database.json.JsonBuilder;
import com.example.Database.cluster.BroadcastService;
import com.example.Database.model.ApiResponse;
import com.example.Database.model.Database;
import com.example.Database.query.command.CommandUtils;
import com.example.Database.query.command.QueryCommand;
import com.example.Database.query.QueryType;
import com.example.Database.services.database.DatabaseService;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class CreateDBCommand implements QueryCommand {

    private final DatabaseService databaseService;
    private final AffinityManager affinityManager;
    private final BroadcastService broadcastService;

    @Autowired
    public CreateDBCommand(DatabaseService databaseService, AffinityManager affinityManager, BroadcastService broadcastService) {
        this.databaseService = databaseService;
        this.affinityManager = affinityManager;
        this.broadcastService = broadcastService;
    }


    @Override
    public QueryType getQueryType() {
        return QueryType.CREATE_DATABASE;
    }

    @Override
    public ApiResponse execute(JSONObject query) {
        try {
            Database database = CommandUtils.getDatabase(query);
            boolean isBroadcasted = "true".equalsIgnoreCase(query.get("X-Broadcast").toString());
            ApiResponse response = databaseService.createDB(database.getDatabaseName());
            if (response.getStatus() == HttpStatus.CREATED && !isBroadcasted) {
                JSONObject details = JsonBuilder.getBuilder()
                        .add("database", database)
                        .add("originatingWorkerPort", affinityManager.getCurrentWorkerPort())
                        .build();
                broadcastOperation(details);
            }
            return response;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void broadcastOperation(JSONObject details) {
        System.out.println("Starting broadcasting database creation to others..");
        Database database = (Database) details.get("database");
        int originatingWorkerPort = (int) details.get("originatingWorkerPort");
        for (int i = 1; i <= affinityManager.getNumberOfNodes(); i++) {
            if (i == originatingWorkerPort) {
                System.out.println("Skipping broadcast to worker " + i + " (origin node)...");
                continue;
            }
            System.out.println("Broadcasting to worker " + i + "...");
            String url = "http://worker" + i + ":9000/api/createDB" + "/" + database.getDatabaseName();
            System.out.println("[BROADCAST] Broadcasting to URL: " + url);
            new BroadcastService.BroadcastRequestBuilder()
                    .withUrl(url)
                    .withMethod(HttpMethod.POST)
                    .isBroadcasted(true)
                    .broadcast();
        }
    }
}
package com.example.Database.query.command.collection;

import com.example.Database.affinity.AffinityManager;
import com.example.Database.json.JsonBuilder;
import com.example.Database.cluster.BroadcastService;
import com.example.Database.model.ApiResponse;
import com.example.Database.model.Collection;
import com.example.Database.model.Database;
import com.example.Database.query.command.CommandUtils;
import com.example.Database.query.command.QueryCommand;
import com.example.Database.query.QueryType;
import com.example.Database.services.database.CollectionService;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;

@Component
public class CreateCollectionCommand implements QueryCommand {

    private final CollectionService collectionService;
    private final AffinityManager affinityManager;
    private final BroadcastService broadcastService;

    @Autowired
    public CreateCollectionCommand(CollectionService collectionService, AffinityManager affinityManager, BroadcastService broadcastService){
        this.collectionService = collectionService;
        this.affinityManager = affinityManager;
        this.broadcastService = broadcastService;
    }

    @Override
    public QueryType getQueryType() {
        return QueryType.CREATE_COLLECTION;
    }

    @Override
    public ApiResponse execute(JSONObject query) {
        try {
            Database database = CommandUtils.getDatabase(query);
            Collection collection = CommandUtils.getCollection(query);
            JSONObject jsonSchema = CommandUtils.getSchemaJson(query);
            boolean isBroadcasted = "true".equalsIgnoreCase(query.get("X-Broadcast").toString());

            ApiResponse response = collectionService.createCollection(database, collection.getCollectionName(), jsonSchema);
            if(response.getStatus() == HttpStatus.CREATED && !isBroadcasted) {
                JSONObject details = JsonBuilder.getBuilder()
                        .add("database", database)
                        .add("collection", collection)
                        .add("originatingWorkerPort", affinityManager.getCurrentWorkerPort())
                        .build();
                broadcastOperation(details);
            }
            return response;
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public void broadcastOperation(JSONObject details) {
        System.out.println("Starting broadcasting collection creation to others..");
        Database database = (Database) details.get("database");
        Collection collection = (Collection) details.get("collection");
        int originatingWorkerPort = (int) details.get("originatingWorkerPort");
        for (int i = 1; i <= affinityManager.getNumberOfNodes(); i++) {
            if (i == originatingWorkerPort) {
                System.out.println("Skipping broadcast to worker " + i + " (origin node)...");
                continue;
            }
            System.out.println("[BROADCAST] Broadcasting to worker " + i + "...");
            String url = "http://worker" + i + ":9000/api/" + database.getDatabaseName() + "/createCol/" + collection.getCollectionName();
            System.out.println("Broadcasting to URL: " + url);

            new BroadcastService.BroadcastRequestBuilder()
                    .withUrl(url)
                    .withMethod(HttpMethod.POST)
                    .isBroadcasted(true)
                    .broadcast();
        }
    }
}
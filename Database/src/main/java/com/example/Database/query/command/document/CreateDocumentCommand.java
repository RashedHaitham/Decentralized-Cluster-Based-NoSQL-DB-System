package com.example.Database.query.command.document;

import com.example.Database.affinity.AffinityManager;
import com.example.Database.json.JsonBuilder;
import com.example.Database.cluster.BroadcastService;
import com.example.Database.cluster.RedirectionService;
import com.example.Database.file.FileService;
import com.example.Database.model.ApiResponse;
import com.example.Database.model.Collection;
import com.example.Database.model.Database;
import com.example.Database.model.Document;
import com.example.Database.query.command.CommandUtils;
import com.example.Database.query.command.QueryCommand;
import com.example.Database.query.QueryType;
import com.example.Database.services.database.DocumentService;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;

@Component
public class CreateDocumentCommand implements QueryCommand {

    private final DocumentService documentService;
    private final AffinityManager affinityManager;
    private final BroadcastService broadcastService;
    private final RedirectionService redirectionService;

    @Autowired
    public CreateDocumentCommand(DocumentService documentService, AffinityManager affinityManager, BroadcastService broadcastService,
                                 RedirectionService redirectionService) {
        this.documentService = documentService;
        this.affinityManager = affinityManager;
        this.broadcastService = broadcastService;
        this.redirectionService = redirectionService;
    }

    @Override
    public QueryType getQueryType() {
        return QueryType.CREATE_DOCUMENT;
    }

    @Override
    public ApiResponse execute(JSONObject query) {
        try {
            Database database = CommandUtils.getDatabase(query);
            Collection collection = CommandUtils.getCollection(query);
            JSONObject documentJson = CommandUtils.getDocumentJson(query);
            documentJson = FileService.addIdToDocument(documentJson);
            Document document = new Document(documentJson);
            document.setId((String) documentJson.get("_id"));
            boolean isBroadcasted = "true".equalsIgnoreCase(query.get("X-Broadcast").toString());

            int workerWithAffinity = affinityManager.getWorkerPort(document.getId());
            int currentWorkerPort = affinityManager.getCurrentWorkerPort();
            if (!isBroadcasted && currentWorkerPort != workerWithAffinity) {
                System.out.println("The current worker is not the one with affinity. Redirecting...");
                document.setReplicated(true); //set replication to true to not change on the content and avoid duplications
                return redirectionService.redirectToWorkerForCreation(database, collection, document, workerWithAffinity);
            }
            ApiResponse response;
            if (isBroadcasted) {
                System.out.println("Document received for replication...");
                response = documentService.createDocument(database, collection, document);
            } else {
                System.out.println("Not broadcasting. Creating document on the current node...");
                response = documentService.createDocument(database, collection, document);
                if (response.getStatus() == HttpStatus.CREATED) {
                    System.out.println("Document created successfully. Broadcasting creation to other nodes...");
                    JSONObject details = JsonBuilder.getBuilder()
                            .add("database", database)
                            .add("collection", collection)
                            .add("document", document)
                            .add("originatingWorkerPort", affinityManager.getCurrentWorkerPort())
                            .build();
                    broadcastOperation(details);
                }
            }
            if (response.getStatus() == HttpStatus.CONFLICT) { // accountNumber already exists in the document
                return response;
            }
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Exception occurred in execute method.", e);
        }
    }


    @Override
    public void broadcastOperation(JSONObject details) {
        Database database = (Database) details.get("database");
        Collection collection = (Collection) details.get("collection");
        Document document = (Document) details.get("document");
        int originatingWorkerPort = (int) details.get("originatingWorkerPort");
        JSONObject documentJson = new JSONObject(document.getContent());
        for (int i = 1; i <= affinityManager.getNumberOfNodes(); i++) {
            if (i == originatingWorkerPort)
                continue; // skip the worker who has done the operation
            String url = "http://worker" + i + ":9000/api/" + database.getDatabaseName() + "/" + collection.getCollectionName() + "/createDoc";
            new BroadcastService.BroadcastRequestBuilder()
                    .withUrl(url)
                    .withMethod(HttpMethod.POST)
                    .isBroadcasted(true)
                    .withData(documentJson)
                    .broadcast();
        }
    }
}
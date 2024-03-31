package com.example.Database.query.command.document;

import com.example.Database.affinity.AffinityManager;
import com.example.Database.json.JsonBuilder;
import com.example.Database.cluster.BroadcastService;
import com.example.Database.cluster.RedirectionService;
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
public class DeleteDocumentCommand implements QueryCommand {

    private final DocumentService documentService;
    private final AffinityManager affinityManager;
    private final BroadcastService broadcastService;
    private final RedirectionService redirectionService;

    @Autowired
    public DeleteDocumentCommand(DocumentService documentService, AffinityManager affinityManager, BroadcastService broadcastService,
                                 RedirectionService redirectionService) {
        this.documentService = documentService;
        this.affinityManager = affinityManager;
        this.broadcastService = broadcastService;
        this.redirectionService = redirectionService;
    }

    @Override
    public QueryType getQueryType() {
        return QueryType.DELETE_DOCUMENT;
    }

    @Override
    public ApiResponse execute(JSONObject query) {
        System.out.println("Starting delete document process...");
        try {
            Database database = CommandUtils.getDatabase(query);
            Collection collection = CommandUtils.getCollection(query);
            String documentId = CommandUtils.getDocumentId(query);
            Document document = new Document(documentId);
            boolean isBroadcasted = "true".equalsIgnoreCase(query.get("X-Broadcast").toString());
            int workerWithAffinity = affinityManager.getWorkerPort(document.getId());
            int currentWorkerPort = affinityManager.getCurrentWorkerPort();
            if (!isBroadcasted && currentWorkerPort != workerWithAffinity) {
                System.out.println("Current worker isn't the one with affinity. Redirecting...");
                return redirectionService.redirectToWorkerForDeletion(database, collection, document, workerWithAffinity);
            }
            if (isBroadcasted) {
                System.out.println("Document received for replication...");
                return documentService.deleteDocument(collection, document);
            } else {
                ApiResponse response = documentService.deleteDocument(collection, document);
                if (response.getStatus() == HttpStatus.ACCEPTED) {
                    System.out.println("Broadcasting deletion to other nodes...");
                    JSONObject details = JsonBuilder.getBuilder()
                            .add("database", database)
                            .add("collection", collection)
                            .add("document", document)
                            .add("originatingWorkerPort", affinityManager.getCurrentWorkerPort())
                            .build();
                    broadcastOperation(details);
                }
                return response;
            }

        } catch (Exception e) {
            System.out.println("Exception occurred in delete process. Message: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public void broadcastOperation(JSONObject details) {
        Database database = (Database) details.get("database");
        Collection collection = (Collection) details.get("collection");
        Document document = (Document) details.get("document");
        int originatingWorkerPort = (int) details.get("originatingWorkerPort");

        for (int i = 1; i <= affinityManager.getNumberOfNodes(); i++) {
            if (i == originatingWorkerPort)
                continue; // Skip the worker which already deleted the document
            System.out.println("broadcasting the operation to worker" + i);
            String url = "http://worker" + i + ":9000/api/" + database.getDatabaseName() + "/" +
                    collection.getCollectionName() + "/deleteDoc?doc_id=" + document.getId();
            new BroadcastService.BroadcastRequestBuilder()
                    .withUrl(url)
                    .withMethod(HttpMethod.DELETE)
                    .isBroadcasted(true)
                    .broadcast();
        }
    }
}
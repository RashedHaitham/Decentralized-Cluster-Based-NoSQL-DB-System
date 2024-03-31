package com.example.Database.query.command.document;

import com.example.Database.affinity.AffinityManager;
import com.example.Database.json.JsonBuilder;
import com.example.Database.cluster.BroadcastService;
import com.example.Database.cluster.RedirectionService;
import com.example.Database.file.DatabaseFileOperations;
import com.example.Database.index.IndexManager;
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

import java.util.HashMap;
import java.util.Map;

@Component
public class UpdateDocumentCommand implements QueryCommand {

    private final DocumentService documentService;
    private final AffinityManager affinityManager;
    private final BroadcastService broadcastService;
    private final RedirectionService redirectionService;
    private final IndexManager indexManager;


    @Autowired
    public UpdateDocumentCommand(DocumentService documentService, AffinityManager affinityManager, BroadcastService broadcastService,
                                 RedirectionService redirectionService){
        this.documentService = documentService;
        this.affinityManager = affinityManager;
        this.broadcastService = broadcastService;
        this.redirectionService = redirectionService;
        this.indexManager = IndexManager.getInstance();
    }

    @Override
    public QueryType getQueryType() {
        return QueryType.UPDATE_DOCUMENT_PROPERTY;
    }

    @Override
    public ApiResponse execute(JSONObject query) {
        System.out.println("Starting update document process...");
        try {
            Database database = CommandUtils.getDatabase(query);
            Collection collection = CommandUtils.getCollection(query);
            String documentId = CommandUtils.getDocumentId(query);
            String propertyName = CommandUtils.getPropertyName(query);
            Object newPropertyValue = CommandUtils.getNewPropertyValue(query);
            Document existingDocument = DatabaseFileOperations.fetchDocumentFromDatabase(documentId, collection.getCollectionName());
            Document documentToUpdate = new Document(documentId);
            assert existingDocument != null;
            documentToUpdate.setVersion(existingDocument.getVersion());
            documentToUpdate.setPropertyName(propertyName);
            documentToUpdate.setPropertyValue(newPropertyValue);
            int workerWithAffinity = affinityManager. getWorkerPort(documentToUpdate.getId());
            int currentWorkerPort = affinityManager.getCurrentWorkerPort();
            boolean isBroadcasted = "true".equalsIgnoreCase(query.get("X-Broadcast").toString());
            if (!isBroadcasted && currentWorkerPort != workerWithAffinity) {
                System.out.println("Current worker isn't the one with affinity. Redirecting...");
                System.out.println("Redirecting request for ID: " + documentId + ". Expected version: " + documentToUpdate.getVersion());
                return redirectionService.redirectToWorkerForUpdate(database, collection, documentToUpdate, workerWithAffinity);
            }
            if (isBroadcasted) {
                System.out.println("Document received for replication...");
                System.out.println("Node " + currentWorkerPort + " received document with ID: " + documentId + ". Current version: " + documentToUpdate.getVersion());
                return documentService.updateDocumentProperty(collection, documentToUpdate);
                } else {
                    ApiResponse response = documentService.updateDocumentProperty(collection, documentToUpdate);
                    if (response.getStatus() == HttpStatus.ACCEPTED) {
                        System.out.println("Broadcasting deletion to other nodes...");
                        JSONObject details = JsonBuilder.getBuilder()
                                .add("database", database)
                                .add("collection", collection)
                                .add("document", documentToUpdate)
                                .add("originatingWorkerPort", affinityManager.getCurrentWorkerPort())
                                .build();
                        System.out.println("Broadcasting deletion to other nodes for ID: " + documentToUpdate.getId() + ". Current version: " + documentToUpdate.getVersion());
                        broadcastOperation(details);
                    }
                    return response;
                }

            } catch (Exception e) {
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
            System.out.println("Node " + originatingWorkerPort + " broadcasting to worker" + i + " for ID: " + document.getId() + ". Current version: " + document.getVersion());
            String url = "http://worker" + i + ":9000/api/" + database.getDatabaseName() + "/"
                    + collection.getCollectionName() + "/updateDoc/" + document.getPropertyName() + "?doc_id=" + document.getId();
            Map<String, String> additionalHeaders = new HashMap<>();
            additionalHeaders.put("newPropertyValue", document.getPropertyValue().toString());
            new BroadcastService.BroadcastRequestBuilder()
                    .withUrl(url)
                    .withMethod(HttpMethod.PUT)
                    .isBroadcasted(true)
                    .withAdditionalHeaders(additionalHeaders)
                    .broadcast();
        }
    }
}
package com.example.Database.query.command.document;

import com.example.Database.model.ApiResponse;
import com.example.Database.model.Collection;
import com.example.Database.model.Document;
import com.example.Database.query.command.CommandUtils;
import com.example.Database.query.command.QueryCommand;
import com.example.Database.query.QueryType;
import com.example.Database.services.database.DocumentService;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class SearchDocumentCommand implements QueryCommand {

    private final DocumentService documentServices;

    @Autowired
    public SearchDocumentCommand(DocumentService documentServices){
        this.documentServices = documentServices;
    }

    @Override
    public QueryType getQueryType() {
        return QueryType.SEARCH;
    }

    @Override
    public ApiResponse execute(JSONObject query) {
        try {
            Collection collection = CommandUtils.getCollection(query);
            String documentId = CommandUtils.getDocumentId(query);
            Document document = new Document(documentId);
            String propertyName = query.get("propertyName").toString();
            return documentServices.searchProperty(collection, document, propertyName);
        } catch (Exception e) {
            return new ApiResponse("Error searching document: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
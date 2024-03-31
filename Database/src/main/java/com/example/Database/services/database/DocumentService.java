package com.example.Database.services.database;

import com.example.Database.affinity.AffinityManager;
import com.example.Database.file.DatabaseFileOperations;
import com.example.Database.file.FileService;
import com.example.Database.index.Index;
import com.example.Database.index.IndexManager;
import com.example.Database.model.ApiResponse;
import com.example.Database.model.Collection;
import com.example.Database.model.Database;
import com.example.Database.model.Document;
import com.example.Database.schema.SchemaValidator;
import com.example.Database.schema.datatype.DataTypeUtil;
import com.example.Database.services.AccountDirectoryService;
import com.example.Database.services.PasswordHashing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.util.*;
import org.json.simple.JSONObject;

@Service
public class DocumentService {

    private final SchemaValidator schemaValidator;
    private final IndexManager indexManager;
    private final AffinityManager affinityManager;
    private final AccountDirectoryService accountDirectoryService;


    @Autowired
    public DocumentService(SchemaValidator schemaValidator, AffinityManager affinityManager, AccountDirectoryService accountDirectoryService) {
        this.schemaValidator = schemaValidator;
        this.indexManager = IndexManager.getInstance();
        this.affinityManager = affinityManager;
        this.accountDirectoryService = accountDirectoryService;
    }

    public ApiResponse createDocument(Database database, Collection collection, Document document) {
        String collectionName = collection.getCollectionName().toLowerCase();

        collection.getDocumentLock().lock();
        try {
            if (!indexManager.indexExists(collectionName)) {
                indexManager.createIndex(collectionName);
            }
            JSONObject jsonData = document.getContent();
            String accountNumber = (jsonData.get("accountNumber") instanceof Long)
                    ? Long.toString((Long) jsonData.get("accountNumber")).trim()
                    : document.getContent().get("accountNumber").toString().trim();
            if (!accountNumber.isEmpty()) {
                System.out.println("acc num in create doc "+accountNumber);
                System.out.println("create doc propIndxExist ?"+collectionName+" "+indexManager.propertyIndexExists(collectionName, "accountNumber"));
                if (!indexManager.propertyIndexExists(collectionName, "accountNumber")) {
                    indexManager.createPropertyIndex(collectionName, "accountNumber");
                }
                String existingDocumentId = indexManager.searchInPropertyIndex(collectionName, "accountNumber", accountNumber);
                System.out.println("create doc existingdocID? "+existingDocumentId);
                if (existingDocumentId != null) {
                    return new ApiResponse("An account with the same account number already exists.", HttpStatus.CONFLICT);
                }
            }
            if (jsonData.containsKey("password") && !document.isReplicated()) {
                String potentialPassword = jsonData.get("password").toString();
                if (!PasswordHashing.isAlreadyHashed(potentialPassword)) {
                    System.out.println("PASSWORD HAVEN'T BEEN PROCESSED YET WAIT UNTIL IT IS HASHED.............");
                    String hashedPassword = PasswordHashing.hashPassword(potentialPassword);
                    jsonData.put("password", hashedPassword);
                }
            }
            if (document.isValidDocument(schemaValidator, collectionName)) {
                String documentId = document.getId();
                int workerPort = affinityManager.getWorkerPort(documentId);   //adding affinity based on the document's id
                document.setHasAffinity(true);
                document.setNodeWithAffinity(workerPort);
                List<String> indexedProperties = Arrays.asList("accountType", "status", "balance", "accountNumber", "clientName");
                for (Object key : jsonData.keySet()) {
                    Object value = jsonData.get(key);
                    if (key != null && value != null && indexedProperties.contains(key.toString())) {
                        String propertyName = key.toString();
                        if (!indexManager.propertyIndexExists(collectionName, propertyName)) {
                            indexManager.createPropertyIndex(collectionName, propertyName);
                        }
                        indexManager.insertIntoPropertyIndex(collectionName, propertyName, value.toString(), document.getId());
                    }
                }
                accountDirectoryService.registerAccount(accountNumber, database.getDatabaseName(), collectionName, documentId);
                ApiResponse response = DatabaseFileOperations.appendDocumentToFile(collectionName, document);
                if (response.getStatus() == HttpStatus.CREATED) {
                    int lastIndex = FileService.getJSONArrayLength(FileService.getCollectionFile(collectionName)) - 1;
                    indexManager.insertIntoIndex(collectionName, document.getId(), lastIndex);
                }
                return response;
            } else {
                return new ApiResponse("Document does not match the schema for " + collectionName, HttpStatus.CONFLICT);
            }
        } finally {
            collection.getDocumentLock().unlock();
        }
    }

    public List<JSONObject> readDocuments(Collection collection) {
        String collectionName = collection.getCollectionName().toLowerCase();
        List<JSONObject> documents = new ArrayList<>();
        collection.getDocumentLock().lock();
        try {
            Index index = indexManager.getIndex(collectionName);
            if (index != null) {
                List<Map.Entry<String, String>> allEntries = index.getBPlusTree().getAllEntries();
                for (Map.Entry<String, String> entry : allEntries) {
                    int documentIndex = Integer.parseInt(entry.getValue());
                    JSONObject document = DatabaseFileOperations.readDocumentFromFile(collectionName, documentIndex);
                    if (document != null) {
                        documents.add(document);
                    }
                }
            }
            return documents;
        } finally {
            collection.getDocumentLock().unlock();
        }
    }

    public ApiResponse deleteDocument(Collection collection, Document document) {
        String collectionName = collection.getCollectionName().toLowerCase();
        collection.getDocumentLock().lock();
        try {
            if (!indexManager.indexExists(collectionName)) {
                indexManager.createIndex(collectionName);
            }
            JSONObject deletedDocument = DatabaseFileOperations.deleteDocument(collectionName, document.getId());
            if (deletedDocument != null) {
                indexManager.deleteFromIndex(collectionName, document.getId());
                List<String> indexedProperties = Arrays.asList("accountNumber", "balance", "accountType", "status");
                for (String propertyName : indexedProperties) {
                    if (deletedDocument.containsKey(propertyName) && indexManager.propertyIndexExists(collectionName, propertyName)) {
                       String propertyValue = deletedDocument.get(propertyName).toString();
                        indexManager.deleteFromPropertyIndex(collectionName, propertyName, propertyValue);
                    }
                }
                String accountNumber = deletedDocument.get("accountNumber").toString();
                accountDirectoryService.deleteAccount(accountNumber);
                return new ApiResponse(accountNumber, HttpStatus.ACCEPTED);
            } else {
                return new ApiResponse("Failed to delete document from " + collectionName, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } finally {
            collection.getDocumentLock().unlock();
        }
    }

    public ApiResponse updateDocumentProperty(Collection collection, Document document) {
        String collectionName = collection.getCollectionName().toLowerCase();
        String propertyName = document.getPropertyName();
        String newPropertyValue = document.getPropertyValue().toString();
        Object castedValue = DataTypeUtil.castToDataType(newPropertyValue, collectionName, propertyName);
        String castedValueString = castedValue.toString();
        collection.getDocumentLock().lock();
        try {
            if (!indexManager.propertyIndexExists(collectionName, propertyName)) {
                indexManager.createPropertyIndex(collectionName, propertyName);
            }
            return DatabaseFileOperations.updateDocumentProperty(collectionName, document, propertyName, castedValueString);
        } finally {
            collection.getDocumentLock().unlock();
        }
    }

    public ApiResponse searchProperty(Collection collection, Document document, String propertyName) {
        System.out.println("searching for " + propertyName + " property in document: " + document.getId());
        String collectionName = collection.getCollectionName().toLowerCase();
        collection.getDocumentLock().lock();
        try {
            String indexResult = indexManager.searchInIndex(collectionName, document.getId()); //search for the document based on the document's id
            if (indexResult == null) {
                return new ApiResponse("Document not found", HttpStatus.NOT_FOUND);
            }
            int index = Integer.parseInt(indexResult);
            JSONObject foundDocument = DatabaseFileOperations.readDocumentFromFile(collectionName, index); //get the document content
            if (foundDocument != null && foundDocument.containsKey(propertyName)) {
                String propertyValueResult = indexManager.searchInPropertyIndex(collectionName, propertyName, document.getId()); //search for the property value
                if (propertyValueResult != null) {
                    return new ApiResponse(propertyValueResult, HttpStatus.OK);
                } else {
                    return new ApiResponse("Property not found", HttpStatus.NOT_FOUND);
                }
            } else {
                return new ApiResponse("Property not found", HttpStatus.NOT_FOUND);
            }
        } finally {
            collection.getDocumentLock().unlock();
        }
    }

    public boolean checkAccountNumberExists(String collection, String accountNumber) {
        String collectionName = collection.toLowerCase();

        try {
            if (!indexManager.indexExists(collectionName)) {
                return false;
            }

            if (!accountNumber.isEmpty()) {

                if (!indexManager.propertyIndexExists(collectionName, "accountNumber")) {
                    return false;
                }
                String existingDocumentId = indexManager.searchInPropertyIndex(collectionName, "accountNumber", accountNumber);
                return existingDocumentId != null;
            }
            return false;
        } finally {
            System.out.println("Account existence checked.");
        }
    }
}
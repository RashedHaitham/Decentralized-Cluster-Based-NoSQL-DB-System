package com.example.Database.file;

import com.example.Database.exceptions.VersionMismatchException;
import com.example.Database.index.IndexManager;
import com.example.Database.model.AccountReference;
import com.example.Database.model.ApiResponse;
import com.example.Database.model.Document;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.http.HttpStatus;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public final class DatabaseFileOperations {

    private DatabaseFileOperations() {}

    public static ApiResponse createDatabase() {
        File dbDirectory = FileService.getDatabasePath();
        if (FileService.isDirectoryExists(dbDirectory)) {
            FileService.createDirectoryIfNotExist(dbDirectory.toPath());
            File schemasDirectory = new File(dbDirectory + "/schemas/");
            FileService.createDirectoryIfNotExist(schemasDirectory.toPath());
            return new ApiResponse("database added successfully", HttpStatus.CREATED);
        } else {
            return new ApiResponse("Database already exists.", HttpStatus.CONFLICT);
        }
    }

    public static ApiResponse deleteDatabase(String dbName) {
        File dbDirectory = FileService.getDatabasePath();
        if (FileService.isDirectoryExists(dbDirectory)) {
            return new ApiResponse("Database does not exist.", HttpStatus.NOT_FOUND);
        }
        Map<String, AccountReference> accountDirectory = FileService.loadAccountDirectory();
        accountDirectory.entrySet().removeIf(entry -> dbName.equals(entry.getValue().getDatabaseName()));
        FileService.saveAccountDirectory(accountDirectory);
        try {
            FileUtils.deleteDirectory(dbDirectory);
            return new ApiResponse("Database, collections, and documents have been successfully deleted.", HttpStatus.ACCEPTED);
        } catch (IOException e) {
            return new ApiResponse("Failed to delete database: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public static List<String> readDatabases() {
        File rootDirectory = FileService.getRootFile();
        if (FileService.isDirectoryExists(rootDirectory)) {
            return Collections.emptyList();
        }
        String[] directories = rootDirectory.list((current, name) -> new File(current, name).isDirectory());
        if (directories == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(directories);
    }

    public static ApiResponse createCollection(String collectionName, JSONObject jsonSchema) {
        File dbDirectory = FileService.getDatabasePath();
        if (FileService.isDirectoryExists(dbDirectory)) {
            return new ApiResponse("Database not found.", HttpStatus.NOT_FOUND);
        }
        File collectionFile = FileService.getCollectionFile(collectionName);
        File schemaFile = FileService.getSchemaPath(collectionName);
        if (FileService.isFileExists(collectionFile.getPath())) {
            return new ApiResponse("Collection already exists.", HttpStatus.CONFLICT);
        }
        try {
            FileService.createDirectoryIfNotExist(schemaFile.getParentFile().toPath());
            Files.write(Paths.get(collectionFile.toURI()), "[]".getBytes());
            Files.write(Paths.get(schemaFile.toURI()), jsonSchema.toString().getBytes());
            return new ApiResponse("Collection has been successfully created", HttpStatus.CREATED);
        } catch (IOException e) {
            e.printStackTrace();
            return new ApiResponse("Failed to create collection: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public static ApiResponse deleteCollection(String collectionName) {
        try {
            File databaseFile = FileService.getDatabasePath();
            if (FileService.isDirectoryExists(databaseFile)) {
                return new ApiResponse("Database not found.", HttpStatus.NOT_FOUND);
            }
            File collectionFile = FileService.getCollectionFile(collectionName);
            File schemaFile = FileService.getSchemaPath(collectionName);
            File indexDirectory = new File(FileService.getDatabasePath() + "/indexes/" + collectionName + "_indexes");
            if (!FileService.isFileExists(collectionFile.getPath()) || !FileService.isFileExists(schemaFile.getPath())) {
                return new ApiResponse("Collection or schema do not exist.", HttpStatus.NOT_FOUND);
            }
            Map<String, AccountReference> accountDirectory = FileService.loadAccountDirectory();
            accountDirectory.entrySet().removeIf(entry -> collectionName.equals(entry.getValue().getCollectionName()));
            FileService.saveAccountDirectory(accountDirectory);
            boolean isCollectionDeleted = collectionFile.delete();
            boolean isSchemaDeleted = schemaFile.delete();
            FileService.deleteDirectoryRecursively(indexDirectory.toPath());
            if (isCollectionDeleted && isSchemaDeleted) {
                return new ApiResponse("Collection, its schema, associated account directory entries, and indexes have been successfully deleted", HttpStatus.ACCEPTED);
            } else {
                return new ApiResponse("Failed to delete collection, associated schema, account directory entries, or index.", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }catch (IOException e){
            e.printStackTrace();
            return new ApiResponse("Failed to delete collection, associated schema, account directory entries, or index.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public static List<String> readCollections() {
        File dbDirectory = FileService.getDatabasePath();
        if (FileService.isDirectoryExists(dbDirectory)) {
            return Collections.emptyList();
        }
        File[] collectionFiles = dbDirectory.listFiles((dir, name) -> name.endsWith(".json"));
        if (collectionFiles == null) {
            return Collections.emptyList();
        }
        List<String> collectionNames = new ArrayList<>();
        for (File collectionFile : collectionFiles) {
            String fileName = collectionFile.getName();
            int extensionIndex = fileName.lastIndexOf(".");
            //if valid extension
            if (extensionIndex >= 0) {
                String collectionName = fileName.substring(0, extensionIndex);
                collectionNames.add(collectionName);
            }
        }
        return collectionNames;
    }

    public static ApiResponse appendDocumentToFile(String collectionName, Document document) {
        File collectionFile = FileService.getCollectionFile(collectionName);
        JSONArray jsonArray = FileService.readJsonArrayFile(collectionFile);
        if (jsonArray == null) {
            return new ApiResponse("Failed to read the existing documents", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        JSONObject documentData = document.getContent();
        documentData.put("_version", 0);
        jsonArray.add(documentData);
        boolean writeStatus = FileService.writeJsonArrayFile(collectionFile.toPath(), jsonArray);
        if (!writeStatus) {
            return new ApiResponse("Failed to append document.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ApiResponse("Document added successfully.", HttpStatus.CREATED);
    }

    public static JSONObject readDocumentFromFile(String collectionName, int index) {
        File collectionFile = FileService.getCollectionFile(collectionName);
        JSONArray jsonArray = FileService.readJsonArrayFile(collectionFile);
        if (jsonArray != null && index >= 0 && index < jsonArray.size()) {
            return (JSONObject) jsonArray.get(index);
        }
        return null;
    }

    private static int getDocumentIndex(String collectionName, String documentId, JSONArray jsonArray) throws Exception {
        File collectionFile = FileService.getCollectionFile(collectionName);
        JSONArray tempArray = FileService.readJsonArrayFile(collectionFile);
        if (tempArray == null) {
            throw new Exception("Failed to read the existing documents for " + collectionName);
        }
        String searchResult = IndexManager.getInstance().searchInIndex(collectionName, documentId);
        if (searchResult == null) {
            throw new Exception("Document not found in " + collectionName);
        }
        jsonArray.addAll(tempArray);
        return Integer.parseInt(searchResult);
    }

    public static JSONObject deleteDocument(String collectionName, String documentId) {
        try {
            JSONArray jsonArray = new JSONArray();
            int index = getDocumentIndex(collectionName, documentId, jsonArray);
            if (index < 0) {
                return null;
            }
            JSONObject removedDocument = (JSONObject) jsonArray.get(index);
            jsonArray.remove(index);
            File collectionFile = FileService.getCollectionFile(collectionName);
            boolean writeStatus = FileService.writeJsonArrayFile(collectionFile.toPath(), jsonArray);
            return writeStatus ? removedDocument : null;
        } catch (Exception e) {
            return null;
        }
    }

    public static ApiResponse updateDocumentProperty(String collectionName, Document document, String propertyName, Object newValue) {
        try {
            String documentId = document.getId();
            JSONArray jsonArray = new JSONArray();
            IndexManager indexManager = IndexManager.getInstance();
            int index = getDocumentIndex(collectionName, documentId, jsonArray);

            if (index < 0) {
                return new ApiResponse("Document with id " + documentId + " not found in " + collectionName, HttpStatus.NOT_FOUND);
            }
            JSONObject documentData = (JSONObject) jsonArray.get(index);
            if (documentData.containsKey("_version") && document.getVersion() != (long) documentData.get("_version")) {
                throw new VersionMismatchException();
            }
            documentData.put(propertyName, newValue);
            documentData.put("_version", document.getVersion() + 1);
            document.setVersion(document.getVersion() + 1);
            boolean writeStatus = FileService.writeJsonArrayFile(FileService.getCollectionFile(collectionName).toPath(), jsonArray);
            if (!writeStatus) {
                return new ApiResponse("Failed to update document.", HttpStatus.INTERNAL_SERVER_ERROR);
            }

            indexManager.deleteFromPropertyIndex(collectionName, propertyName, documentId);
            indexManager.insertIntoPropertyIndex(collectionName, propertyName, newValue.toString(), documentId);

            return new ApiResponse("Document with id " + documentId + " updated successfully in " + collectionName, HttpStatus.ACCEPTED);
        } catch (Exception e) {
            return new ApiResponse("Error updating document property: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public static JSONObject fetchDocumentById(String collectionName, String documentId) {
        try {
            JSONArray jsonArray = new JSONArray();
            int index = getDocumentIndex(collectionName, documentId, jsonArray);
            if (index < 0) {
                return null;
            }
            return (JSONObject) jsonArray.get(index);
        } catch (Exception e) {
            return null;
        }
    }

    public static Document fetchDocumentFromDatabase(String documentId, String collectionName) {
        JSONObject jsonObject = fetchDocumentById(collectionName, documentId);
        if (jsonObject == null) {
            return null;
        }
        Document document = new Document(documentId);
        if (jsonObject.containsKey("_version")) {
            document.setVersion(Integer.parseInt(jsonObject.get("_version").toString()));
        }
        return document;
    }
}
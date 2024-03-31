package com.example.Database.services.database;

import com.example.Database.file.DatabaseFileOperations;
import com.example.Database.model.ApiResponse;
import org.json.simple.JSONObject;
import org.springframework.stereotype.Service;
import com.example.Database.model.Database;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class CollectionService {

    public ApiResponse createCollection(Database database, String collectionName, JSONObject jsonSchema) {
        database.getCollectionLock().lock();
        try {
            //soft addition
            database.createCollection(collectionName);
            //hard addition
            return DatabaseFileOperations.createCollection(collectionName, jsonSchema);
        } finally {
            database.getCollectionLock().unlock();
        }
    }

    public ApiResponse deleteCollection(Database database, String collectionName) {
        database.getCollectionLock().lock();
        try {
            //soft delete
            database.deleteCollection(collectionName);
            //hard delete
            return DatabaseFileOperations.deleteCollection(collectionName);
        } finally {
            database.getCollectionLock().unlock();
        }
    }

    public List<String> readCollections(Database database) {
        Set<String> uniqueCollections = new HashSet<>();
        database.getCollectionLock().lock();
        try {
            List<String> inMemoryCollections = database.readCollections();
            uniqueCollections.addAll(inMemoryCollections);
            List<String> inFileCollections = DatabaseFileOperations.readCollections();
            uniqueCollections.addAll(inFileCollections);
            return new ArrayList<>(uniqueCollections);
        } finally {
            database.getCollectionLock().unlock();
        }
    }
}
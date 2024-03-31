package com.example.Database.model;

import com.example.Database.index.IndexManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemoryDatabase {
    private final Map<String, Database> databases;
    private static InMemoryDatabase instance;

    private InMemoryDatabase() {
        this.databases = new HashMap<>();
    }

    public static InMemoryDatabase getInstance() {
        if (instance == null)
            instance = new InMemoryDatabase();
        return instance;
    }

    public synchronized void createDatabase(String databaseName) {
        if (databaseName == null || databaseName.trim().isEmpty()) {
            throw new IllegalArgumentException("Database name cannot be null or empty.");
        }
        if (databases.containsKey(databaseName)) {
            return;
        }
        Database database = new Database(databaseName);
        databases.put(databaseName, database);
    }

    public Database getOrCreateDatabase(String databaseName) {
        if (databaseName == null || databaseName.trim().isEmpty()) {
            throw new IllegalArgumentException("Database name cannot be null or empty.");
        }
        return databases.computeIfAbsent(databaseName, Database::new);
    }

    public List<String> readDatabases() {
        return new ArrayList<>(databases.keySet());
    }

    public void deleteDatabase(String databaseName) {
        if (databaseName == null || databaseName.trim().isEmpty()) {
            throw new IllegalArgumentException("Database name cannot be null or empty.");
        }
        databases.remove(databaseName);
        IndexManager.getInstance().deleteAllIndexes(); //clear all the indexes if
    }
}
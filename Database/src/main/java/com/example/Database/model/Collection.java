package com.example.Database.model;

import java.util.concurrent.locks.ReentrantLock;

public class Collection {
    private final ReentrantLock documentLock;
    private final String collectionName;

    public Collection(String collectionName) {
        this.collectionName = collectionName;
        documentLock = new ReentrantLock();
    }

    public String getCollectionName() {
        return collectionName;
    }

    public ReentrantLock getDocumentLock() {
        return documentLock;
    }
}
package com.example.Database.index;

import com.example.Database.file.FileService;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class IndexManager {

    // ConcurrentHashMap to allow safe multi-threaded access to indexes
    private final Map<String, Index> indexMap = new ConcurrentHashMap<>();
    private final Map<String, PropertyIndex> propertyIndexMap = new ConcurrentHashMap<>();
    private static IndexManager instance;

    private IndexManager(){
        List<String> allDatabases = FileService.getAllKnownDatabases();
        for (String dbName : allDatabases) {
            FileService.setDatabaseDirectory(dbName);
            this.loadAllIndexes();
        }
    }

    public static IndexManager getInstance(){
        if(instance == null){
            instance = new IndexManager();
        }
        return instance;
    }

    public void loadAllIndexes() {
        System.out.println("Loading all indexes...");
        File databasePath = new File(FileService.getDatabasePath().toURI());
        File indexesDirectory = new File(databasePath, "indexes");
        if(!indexesDirectory.exists()){
            return;
        }
        String[] collectionDirectories = indexesDirectory.list();
        if (collectionDirectories != null) {
            for (String collectionDir : collectionDirectories) {
                File currentDir = new File(indexesDirectory, collectionDir);
                String[] filesInDir = currentDir.list();
                if (filesInDir != null) {
                    for (String indexFile : filesInDir) {
                        if (FileService.isPropertyIndexFile(indexFile)) {
                            this.loadPropertyIndexForCollection(indexFile);
                        } else if (FileService.isIndexFile(indexFile)) {
                            this.loadIndexForCollection(indexFile);
                        }
                    }
                }
            }
        } else {
            System.out.println("No collections found.");
        }
    }

    private void loadIndexForCollection(String collectionFileName) {
        String cleanCollectionName = collectionFileName.substring(0, collectionFileName.length() - 10);
        String indexPath = FileService.getIndexFilePath(cleanCollectionName);
        Index index = new Index();
        indexMap.put(cleanCollectionName, index);
        this.loadFromFile(indexPath, index);
    }

    private void loadPropertyIndexForCollection(String collectionFileName) {
        String cleanCollectionName = collectionFileName.substring(0, collectionFileName.length() - 17);
        String[] split = cleanCollectionName.split("_");
        String derivedCollectionName = split[0];
        String property = split[1];
        String propertyIndexPath = FileService.getPropertyIndexFilePath(derivedCollectionName, property);
        PropertyIndex propertyIndex = new PropertyIndex();
        propertyIndexMap.put(derivedCollectionName + "_" + property, propertyIndex);
        loadFromFile(propertyIndexPath, propertyIndex);
    }

    private void loadFromFile(String path, Object index) {
        if (!FileService.isFileExists(path)) {
            return;
        }
        Map<String, String> indexData = FileService.readIndexFile(path);
        for (Map.Entry<String, String> entry : indexData.entrySet()) {
            if(index instanceof Index) {
                ((Index) index).insert(entry.getKey(), entry.getValue());
            } else if(index instanceof PropertyIndex) {
                ((PropertyIndex) index).insert(entry.getKey(), entry.getValue());
            }
        }
    }

    public void deleteAllIndexes(){
        indexMap.clear();
        propertyIndexMap.clear();
    }

    public Index getIndex(String collectionName) {
        Index index = indexMap.get(collectionName);
        if (index == null) {
            throw new IllegalArgumentException("Index does not exist.");
        }
        return index;
    }


    public void createIndex(String collectionName) {
        if (!indexMap.containsKey(collectionName)) {
            Index index = new Index();
            indexMap.put(collectionName, index);
        }
    }

    public void insertIntoIndex(String collectionName, String documentId, int index) {
        String existingValue = getIndex(collectionName).search(documentId);
        if (existingValue == null) {
            getIndex(collectionName).insert(documentId, String.valueOf(index));
            FileService.appendToIndexFile(FileService.getIndexFilePath(collectionName), documentId, String.valueOf(index));
        }
    }

    public void deleteFromIndex(String collectionName, String documentId) {
        Index index = getIndex(collectionName);
        int deletedIndex;
        List<Map.Entry<String, String>> allEntries = new ArrayList<>(index.getBPlusTree().getAllEntries());
        String indexValue = index.search(documentId);
        if (indexValue != null) {
            deletedIndex = Integer.parseInt(indexValue);
        } else {
            throw new IllegalArgumentException("Document not found in the index.");
        }
        index.delete(documentId);
        Map<String, String> updatedEntries = new HashMap<>();
        for (Map.Entry<String, String> entry : allEntries) {
            int currentIndex = Integer.parseInt(entry.getValue());
            if (currentIndex > deletedIndex) {
                updatedEntries.put(entry.getKey(), String.valueOf(currentIndex - 1));
            } else {
                updatedEntries.put(entry.getKey(), String.valueOf(currentIndex));
            }
        }
        updatedEntries.remove(documentId);
        index.getBPlusTree().clearTree();
        for (Map.Entry<String, String> entry : updatedEntries.entrySet()) {
            index.insert(entry.getKey(), entry.getValue());
        }
        FileService.rewriteIndexFile(collectionName, index);
    }

    public String searchInIndex(String collectionName, String documentId) {
        return getIndex(collectionName).search(documentId);
    }

    public void createPropertyIndex(String collectionName, String propertyName) {
        String propertyIndexKey = collectionName + "_" + propertyName;
        if (!propertyIndexMap.containsKey(propertyIndexKey)) {
            PropertyIndex index = new PropertyIndex();
            propertyIndexMap.put(propertyIndexKey, index);
        }
    }

    public void insertIntoPropertyIndex(String collectionName, String propertyName, String propertyValue, String documentId) {
        String propertyIndexKey = collectionName + "_" + propertyName;
        PropertyIndex propertyIndex = propertyIndexMap.get(propertyIndexKey);
        if (propertyIndex == null) {
            throw new IllegalArgumentException("Property Index does not exist");
        }
        String combinedKey = propertyName.equals("accountNumber") ? propertyValue : documentId + "_" + propertyName;  // Handle account number differently
        String existingPropertyValue = propertyIndex.search(combinedKey);
        if (!propertyValue.equals(existingPropertyValue)) {
            System.out.println("Inserted new entry in property index for collection: " + collectionName + " property: " + propertyName);
            propertyIndex.insert(combinedKey, propertyValue);
            FileService.appendToIndexFile(FileService.getPropertyIndexFilePath(collectionName, propertyName), combinedKey, propertyValue);
        } else {
            System.out.println("Entry already exists in property index for collection: " + collectionName + " property: " + propertyName);
        }
    }

    public String searchInPropertyIndex(String collectionName, String propertyName, String documentId) {
        String propertyIndexKey = collectionName + "_" + propertyName;
        PropertyIndex propertyIndex = propertyIndexMap.get(propertyIndexKey);
        if (propertyIndex == null) {
            throw new IllegalArgumentException("Property Index does not exist.");
        }
        String combinedKey = propertyName.equals("accountNumber") ? documentId : documentId + "_" + propertyName;  // Handle account number differently
        System.out.println(propertyIndex.search(combinedKey));
        return propertyIndex.search(combinedKey);
    }

    public void deleteFromPropertyIndex(String collectionName, String propertyName, String documentId) {
        String propertyIndexKey = collectionName + "_" + propertyName;
        PropertyIndex propertyIndex = propertyIndexMap.get(propertyIndexKey);
        if (propertyIndex == null) {
            throw new IllegalArgumentException("Property Index does not exist.");
        }
        String combinedKey = propertyName.equals("accountNumber") ? documentId : documentId + "_" + propertyName;  // Handle account number differently
        propertyIndex.delete(combinedKey);
        FileService.rewritePropertyIndexFile(FileService.getPropertyIndexFilePath(collectionName, propertyName), propertyIndex);
    }

    public boolean propertyIndexExists(String collectionName, String propertyName) {
        String propertyIndexKey = collectionName + "_" + propertyName;
        return propertyIndexMap.containsKey(propertyIndexKey);
    }


    public boolean indexExists(String collectionName) {
        return indexMap.containsKey(collectionName);
    }
}
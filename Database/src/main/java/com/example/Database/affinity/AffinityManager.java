package com.example.Database.affinity;

import org.springframework.stereotype.Service;

@Service
public class AffinityManager {
    private int currentWorkerNumber;
    private static final int NUMBER_OF_NODES = 4;

    public int getNumberOfNodes(){
        return NUMBER_OF_NODES;
    }

    public int getCurrentWorkerPort() {
        return currentWorkerNumber;
    }

    public synchronized void setCurrentWorkerPort(String currentWorkerName) {
        System.out.println(currentWorkerName);
        try {
            this.currentWorkerNumber = Integer.parseInt(currentWorkerName.trim().replace("worker", ""));
            System.out.println(currentWorkerNumber);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid worker name format.");
        }
    }

    public int getWorkerPort(String documentId) {
        int index = calculateWorkerIndex(documentId);
        return index + 1;
    }

    public int calculateWorkerIndex(String documentId) {
        int hashCode = documentId.hashCode();
        return Math.abs(hashCode) % NUMBER_OF_NODES;
    }
}
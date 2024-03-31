package com.example.Database.index;

import com.example.Database.index.BPlusTree.BPlusTree;

public class Index {
    private BPlusTree<String, String> bPlusTree;

    public Index() {
        this.bPlusTree = new BPlusTree<>();
    }

    public BPlusTree<String, String> getBPlusTree() {
        return bPlusTree;
    }

    public void setBPlusTree(BPlusTree<String, String> bPlusTree) {
        this.bPlusTree = bPlusTree;
    }

    public void insert(String key, String value) {
        bPlusTree.insert(key, value);
    }

    public void delete(String key) {
        bPlusTree.delete(key);
    }

    public String search(String key) {
        return bPlusTree.search(key);
    }
}
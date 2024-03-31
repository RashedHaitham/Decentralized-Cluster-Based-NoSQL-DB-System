package com.example.Database.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AccountReference {
    private final String databaseName;
    private final String collectionName;
    private final String documentId;
}
package com.example.Database.services;

import com.example.Database.file.FileService;
import com.example.Database.model.AccountReference;
import com.example.Database.model.ApiResponse;

import com.example.Database.query.QueryManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class AuthenticationService {
    private final AccountDirectoryService accountDirectoryService;
    private final QueryManager queryManager;

    @Autowired
    public AuthenticationService(AccountDirectoryService accountDirectoryService, QueryManager queryManager) {
        this.accountDirectoryService = accountDirectoryService;
        this.queryManager = queryManager;
    }

    public boolean isAdmin(String username, String password) {
        if (username == null || password == null) {
            throw new RuntimeException("username or token is null");
        }
        String path = FileService.getUserJsonPath("admin");
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode rootNode = objectMapper.readTree(new File(path));
            String storedUsername = rootNode.path("username").asText();
            String storedPassword = rootNode.path("password").asText();
            String hashedPassword = PasswordHashing.isAlreadyHashed(password) ? password : PasswordHashing.hashPassword(password);
            return storedUsername.equals(username) && storedPassword.equals(hashedPassword);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public ApiResponse verifyAdminCredentials(String username, String password) {
        return verifyCredentials("admin", username, password);
    }

    public ApiResponse verifyCustomerCredentials(String accountNumber, String password) {
        return verifyCredentials("customers", accountNumber, password);
    }

    private ApiResponse verifyCredentials(String userType, String identity, String password) {
        ObjectMapper objectMapper = new ObjectMapper();
        String path = FileService.getUserJsonPath(userType);
        String identityField = userType.equals("admin") ? "username" : "accountNumber";
        String hashedPassword = PasswordHashing.hashPassword(password); //To compare against stored hash password

        try {
            // admin
            if (userType.equals("admin")) {
                JsonNode rootNode = objectMapper.readTree(new File(path));
                String storedIdentity = rootNode.path(identityField).asText();
                String storedPassword = rootNode.path("password").asText();
                if (identity.equals(storedIdentity) && hashedPassword.equals(storedPassword)) {
                    return new ApiResponse("Authenticated", HttpStatus.OK);
                } else {
                    return new ApiResponse("Wrong Username or Password", HttpStatus.BAD_REQUEST);
                }
            }
            // customer
            else {
                boolean customerExists = false;

                JsonNode[] rootNodes = objectMapper.readValue(new File(path), JsonNode[].class);
                for (JsonNode rootNode : rootNodes) {
                    String storedIdentity = rootNode.path(identityField).asText();
                    String storedPassword = rootNode.path("password").asText();
                    if (identity.equals(storedIdentity) && hashedPassword.equals(storedPassword)) {
                        if (checkIfClosed(identity)){
                            return new ApiResponse("Account closed,contact your bank to re-open.", HttpStatus.BAD_REQUEST);
                        }
                        return new ApiResponse("Authenticated", HttpStatus.OK);
                    }
                    if (identity.equals(storedIdentity) || hashedPassword.equals(storedPassword)) {
                        customerExists = true;
                    }
                }
                if (customerExists) {
                    return new ApiResponse("Wrong Account Number or Password", HttpStatus.CONFLICT);
                } else {
                    return new ApiResponse("Customer does not exist, \nplease contact your bank or try again.", HttpStatus.NOT_FOUND);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse("Server error", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private boolean checkIfClosed(String identity){
        AccountReference accountReference = accountDirectoryService.getAccountLocation(identity);
        String dbName = accountReference.getDatabaseName();
        String collectionName = accountReference.getCollectionName();
        String documentId = accountReference.getDocumentId();
        String propertyName = "status";

        ApiResponse response = queryManager.searchForProperty(
                dbName,
                collectionName,
                documentId,
                propertyName
        );
        return response.getMessage().equals("false");
    }
}
package com.example.Database.cluster;

import com.example.Database.file.FileService;
import com.example.Database.model.ApiResponse;
import com.example.Database.model.Collection;
import com.example.Database.model.Database;
import com.example.Database.model.Document;
import com.fasterxml.jackson.databind.JsonNode;
import org.json.simple.JSONObject;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

@Service
public class RedirectionService {

    private final RestTemplate restTemplate = new RestTemplate();

    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        JsonNode adminCredentials = FileService.readAdminCredentialsFromJson();
        headers.set("username", adminCredentials.get("username").asText());
        headers.set("password", adminCredentials.get("password").asText());
        return headers;
    }

    public ApiResponse redirectToWorkerForCreation(Database database, Collection collection, Document document, int workerPort) {
        try {
            System.out.println("[INFO] Starting redirectToNodeWithAffinity for creation method...");
            HttpHeaders headers = getHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String url = "http://worker" + workerPort + ":9000/api/" + database.getDatabaseName()
                    + "/" + collection.getCollectionName() + "/createDoc";
            System.out.println("Constructed redirect URL: " + url);
            HttpEntity<String> requestEntity = new HttpEntity<>(document.getContent().toString(), headers);
            ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            return new ApiResponse(responseEntity.getBody(), (HttpStatus) responseEntity.getStatusCode());
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            String errorMsg = e.getResponseBodyAsString();
            HttpStatus status = (HttpStatus) e.getStatusCode();
            System.out.println("Error during redirect: " + status + " - " + errorMsg);
            return new ApiResponse(errorMsg, status);
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse("Unexpected error occurred during redirection", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ApiResponse redirectToWorkerForDeletion(Database database, Collection collection, Document document, int workerPort) {
        System.out.println("[INFO] Starting redirectToNodeWithAffinity for deletion method...");
        HttpHeaders headers = getHeaders();
        String url = "http://worker" + workerPort + ":9000/api/" + database.getDatabaseName() + "/"
                + collection.getCollectionName() + "/deleteDoc?doc_id=" + document.getId();
        System.out.println("Constructed redirect URL: " + url);
        HttpEntity<JSONObject> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.DELETE, requestEntity, String.class);
        return new ApiResponse(responseEntity.getBody(), (HttpStatus) responseEntity.getStatusCode());
    }

    public ApiResponse redirectToWorkerForUpdate(Database database, Collection collection, Document document, int workerPort) {
        System.out.println("[INFO] Starting redirectToNodeWithAffinity for update method...");
        HttpHeaders headers = getHeaders();
        headers.set("newPropertyValue", document.getPropertyValue().toString());
        String url = "http://worker" + workerPort + ":9000/api/" + database.getDatabaseName() + "/"
                + collection.getCollectionName() + "/updateDoc/" + document.getPropertyName() + "?doc_id=" + document.getId();
        System.out.println("Constructed redirect URL: " + url);
        HttpEntity<JSONObject> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.PUT, requestEntity, String.class);
        return new ApiResponse(responseEntity.getBody(), (HttpStatus) responseEntity.getStatusCode());
    }
}
package com.example.BankingSystem.services;

import com.example.BankingSystem.Model.Admin;
import org.springframework.stereotype.Service;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import lombok.SneakyThrows;
import org.springframework.core.ParameterizedTypeReference;
import java.util.Collections;
import java.util.List;

@Service
public class CollectionService {

    public ResponseEntity<String> createCollection(String dbName, String collectionName, HttpSession session) {
        Admin admin = (Admin) session.getAttribute("login");
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("username", admin.getUsername());
        headers.set("password", admin.getPassword());
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        String url = "http://worker1:9000/api/" + dbName + "/createCol/" + collectionName;
        try {
            return restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
    }

    public ResponseEntity<String> deleteCollection(String dbName, String collectionName, HttpSession session) {
        Admin admin = (Admin) session.getAttribute("login");
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("username", admin.getUsername());
        headers.set("password", admin.getPassword());
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        String url = "http://worker1:9000/api/" + dbName + "/deleteCol/" + collectionName;
        try {
            return restTemplate.exchange(url, HttpMethod.DELETE, requestEntity, String.class);
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
    }

    public List<String> fetchExistingCollections(String dbName, HttpSession session) {
        Admin admin = (Admin) session.getAttribute("login");
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("username", admin.getUsername());
        headers.set("password", admin.getPassword());
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        String url = "http://worker1:9000/api/fetchExistingCollections/" + dbName;
        ResponseEntity<List<String>> responseEntity = restTemplate.exchange(url, HttpMethod.GET, requestEntity, new ParameterizedTypeReference<>() {
        });
        return responseEntity.getBody();
    }


    public List<String> getAllCollections(String dbName, HttpSession session) {
        List<String> collectionNames = this.fetchExistingCollections(dbName, session);
        if (collectionNames.isEmpty()) {
            return Collections.emptyList();
        }
        collectionNames.sort(String::compareTo);
        return collectionNames;
    }
}
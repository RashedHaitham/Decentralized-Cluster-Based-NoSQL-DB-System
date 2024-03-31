package com.example.BankingSystem.services;

import com.example.BankingSystem.Model.Admin;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import lombok.SneakyThrows;
import java.util.Collections;
import java.util.List;

@Service
public class DatabaseService {

    public ResponseEntity<String> createDatabase(String db_name, HttpSession session) {
        Admin admin = (Admin) session.getAttribute("login");
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("username", admin.getUsername());
        headers.set("password", admin.getPassword());
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        String url = "http://worker1:9000/api/createDB/" + db_name;
        try {
            return restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
    }

    public ResponseEntity<String> deleteDatabase(String db_name, HttpSession session) {
        Admin admin = (Admin) session.getAttribute("login");
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("username", admin.getUsername());
        headers.set("password", admin.getPassword());
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        String url = "http://worker1:9000/api/deleteDB/" + db_name;
        try {
            return restTemplate.exchange(url, HttpMethod.DELETE, requestEntity, String.class);
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
    }

    public List<String> fetchExistingDatabases(HttpSession session) {
        Admin admin = (Admin) session.getAttribute("login");
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("username", admin.getUsername());
        headers.set("password", admin.getPassword());
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        String url = "http://worker1:9000/api/fetchExistingDatabases";
        ResponseEntity<List<String>> responseEntity = restTemplate.exchange(url, HttpMethod.GET, requestEntity, new ParameterizedTypeReference<>() {
        });
        return responseEntity.getBody();
    }

    public List<String> getAllDatabases(HttpSession session) {
        List<String> databaseNames = this.fetchExistingDatabases(session);
        if (databaseNames.isEmpty()) {
            return Collections.emptyList();
        }
        databaseNames.sort(String::compareTo);
        return databaseNames;
    }
}
package com.example.BankingSystem.services;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;

@Service
public class AuthenticationService {

    public ResponseEntity<String> checkAdmin(String username, String password) {
        return checkCredentials("1", "username", username, password, "/api/check/admin");
    }

    public ResponseEntity<String> checkCustomer(String accountNumber, String password) {
        String worker = getWorker(accountNumber);

        if (worker == null || worker.contains("not found")) {
            return new ResponseEntity<>("Customer does not exist,\n please contact your bank or try again.", HttpStatus.BAD_REQUEST);
        }
        return checkCredentials(worker, "accountNumber", accountNumber, password, "/api/check/customer");
    }

    public String getWorker(String identity) {
        RestTemplate restTemplate = new RestTemplate();
        String url = "http://host.docker.internal:8081/bootstrapper/getWorker/" + identity;
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> request = new HttpEntity<>(headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            return response.getBody();
        } catch (HttpClientErrorException.NotFound e) {
            return "not found";
        } catch (HttpClientErrorException e) {
            return e.getStatusCode().toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }
    }

    private ResponseEntity<String> checkCredentials(String workerId, String identityType, String identity, String password, String url) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set(identityType, identity);
        headers.set("password", password);
        HttpEntity<String> request = new HttpEntity<>(headers);
        String customURL = "http://worker" + workerId + ":9000" + url;
        try {
            return restTemplate.exchange(customURL, HttpMethod.GET, request, String.class);
        } catch (HttpClientErrorException e) {
            return new ResponseEntity<>(e.getResponseBodyAsString(), e.getStatusCode());
        } catch (Exception e) {
            return new ResponseEntity<>("Internal Server Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
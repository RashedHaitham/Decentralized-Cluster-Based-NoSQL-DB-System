package com.example.BankingSystem.services;

import com.example.BankingSystem.Model.Admin;
import com.example.BankingSystem.Model.Customer;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class UserService {

    @PostConstruct
    public void init() {
        registerAdminWithBootstrapper();
    }

    private void registerAdminWithBootstrapper() {
        String url = "http://host.docker.internal:8081/bootstrapper/add/admin";
        HttpHeaders headers = new HttpHeaders();
        Admin admin = new Admin("admin", "admin@12345");
        headers.set("username", admin.getUsername());
        headers.set("password", admin.getPassword());
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();
        try {
            System.out.println("sending user with username " + admin.getUsername() + " to be added to a node");
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            System.out.println("Response from Bootstrapper: " + response.getBody());
        } catch (Exception e) {
            System.out.println("Failed to register admin with Bootstrapper");
            e.printStackTrace();
        }
    }

    public void addCustomer(long accountNumber, String password, HttpSession session) {
        Admin admin = (Admin) session.getAttribute("login");
        Customer customer = new Customer(accountNumber, password);
        String url = "http://host.docker.internal:8081/bootstrapper/add/customer";
        HttpHeaders headers = new HttpHeaders();
        headers.set("accountNumber", String.valueOf(accountNumber));
        headers.set("password", customer.getPassword());
        headers.set("adminUsername", admin.getUsername());
        headers.set("adminPassword", admin.getPassword());
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();
        System.out.println("sending customer with account number " + customer.getAccountNumber() + " to be added to a node");
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
        System.out.println("Response from bootstrapper: " + response.getBody());
    }

    public void updateCustomer(long accountNumber, String password, HttpSession session) {
        Admin admin = (Admin) session.getAttribute("login");
        Customer customer = new Customer(accountNumber, password);
        String url = "http://host.docker.internal:8081/bootstrapper/update/customer";
        HttpHeaders headers = new HttpHeaders();
        headers.set("accountNumber", String.valueOf(accountNumber));
        headers.set("password", customer.getPassword());
        headers.set("adminUsername", admin.getUsername());
        headers.set("adminPassword", admin.getPassword());
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();
        System.out.println("sending customer with account number " + customer.getAccountNumber() + " to be updated to a node");
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, requestEntity, String.class);
        System.out.println("Response from bootstrapper: " + response.getBody());
    }

    public void deleteCustomer(String accountNumber, HttpSession session) {
        Admin admin = (Admin) session.getAttribute("login");
        String url = "http://host.docker.internal:8081/bootstrapper/delete/customer";
        HttpHeaders headers = new HttpHeaders();
        headers.set("accountNumber", accountNumber);
        headers.set("adminUsername", admin.getUsername());
        headers.set("adminPassword", admin.getPassword());
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();
        System.out.println("sending customer with account number " + accountNumber + " to be deleted from a node");
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.DELETE, requestEntity, String.class);
        System.out.println("Response from bootstrapper: " + response.getBody());
    }
}
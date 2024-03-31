package com.example.Bootstrapper.services;

import com.example.Bootstrapper.File.FileServices;
import com.example.Bootstrapper.model.Admin;
import com.example.Bootstrapper.model.Customer;
import com.example.Bootstrapper.model.Node;
import com.example.Bootstrapper.loadbalancer.LoadBalancer;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Service
public class UserService {

    private final LoadBalancer loadBalancer;

    @Autowired
    public UserService(LoadBalancer loadBalancer){
        this.loadBalancer = loadBalancer;
    }

    public void addCustomer(Customer customer) {
        Node node = loadBalancer.assignUserToNextNode(customer.getAccountNumber()); //assign the user to a worker node
        String hashedPassword = PasswordHashing.hashPassword(customer.getPassword()); //hashing the password for security
        customer.setPassword(hashedPassword);
        FileServices.saveUserToJson("customers", customer.toJson()); //adding the customer to json file
        String url = "http://" + node.getNodeIP() + ":9000/api/add/customer";
        HttpHeaders headers = new HttpHeaders();
        headers.set("accountNumber", customer.getAccountNumber());
        headers.set("password", customer.getPassword());
        headers.set("adminUsername", "admin");
        headers.set("adminPassword", "admin@12345");
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();
        System.out.println("sending customer with account number " + customer.getAccountNumber());
        //adding the users into the database worker nodes
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
        System.out.println("Response from database: " + response.getBody());
    }

    public void deleteCustomer(String accountNumber) {
        FileServices.deleteUserFromJson("customers", accountNumber); //delete user from bootstrapper's side
        loadBalancer.balanceExistingUsers();
        String url = "http://" + loadBalancer.getUserNode(accountNumber).getNodeIP() + ":9000/api/delete/customer";
        System.out.println(url);
        Optional<Admin> adminCredentialsOpt = FileServices.getAdminCredentials();
        Admin adminCredentials = adminCredentialsOpt.get();
        HttpHeaders headers = new HttpHeaders();
        headers.set("accountNumber", accountNumber);
        headers.set("adminUsername", adminCredentials.getUsername());
        headers.set("adminPassword", adminCredentials.getPassword());
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();
        //delete from database side too
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.DELETE, requestEntity, String.class);
        System.out.println("Response from Database: " + response.getBody());
    }

    public void updateCustomer(String accountNumber,String password) {
        String HashedPassword=PasswordHashing.hashPassword(password);
        FileServices.updateUserFromJson("customers", accountNumber,HashedPassword); //update user from bootstrapper's side
        String url = "http://" + loadBalancer.getUserNode(accountNumber).getNodeIP() + ":9000/api/update/customer";
        System.out.println(url);
        Optional<Admin> adminCredentialsOpt = FileServices.getAdminCredentials();
        Admin adminCredentials = adminCredentialsOpt.get();
        HttpHeaders headers = new HttpHeaders();
        headers.set("accountNumber", accountNumber);
        headers.set("password", HashedPassword);
        headers.set("adminUsername", adminCredentials.getUsername());
        headers.set("adminPassword", adminCredentials.getPassword());
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();
        //update from database side too
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, requestEntity, String.class);
        System.out.println("Response from Database: " + response.getBody());
    }

    public void addAdmin(Admin admin) {
        System.out.println(admin.getPassword());
        String hashedPassword = PasswordHashing.hashPassword(admin.getPassword()); //adding the admin to json file
        System.out.println(PasswordHashing.hashPassword(admin.getPassword()));
        System.out.println(hashedPassword + " hashed password in bootstrapper");
        admin.setPassword(hashedPassword);
        FileServices.saveAdminToJson(admin.toJson());
        for(int i = 1; i <= 4; i++) {
            Node node = loadBalancer.assignUserToNextNode(admin.getUsername());
            String url = "http://" + node.getNodeIP() + ":9000/api/add/admin";
            HttpHeaders headers = new HttpHeaders();
            headers.set("username", admin.getUsername());
            headers.set("password", admin.getPassword());
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            RestTemplate restTemplate = new RestTemplate();
            boolean success = false;
            while (!success) {
                try {
                    ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
                    if (response.getStatusCode() == HttpStatus.OK) {
                        success = true;
                        System.out.println("Admin added successfully to node " + node.getNodeNumber());
                    } else {
                        System.err.println("Failed to add admin to node " + node.getNodeNumber() + ". Response: " + response.getBody());
                    }
                } catch (Exception e) {
                    System.err.println("Exception occurred while adding admin to node " + node.getNodeNumber());
                    e.printStackTrace();
                }
                if (!success) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }
}
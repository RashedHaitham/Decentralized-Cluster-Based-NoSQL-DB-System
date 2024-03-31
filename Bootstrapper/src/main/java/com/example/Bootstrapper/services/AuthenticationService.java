package com.example.Bootstrapper.services;

import com.example.Bootstrapper.File.FileServices;
import com.example.Bootstrapper.model.Admin;
import com.example.Bootstrapper.model.Customer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Optional;

@Service
public class AuthenticationService {

    public boolean isAdmin(String username, String password) {
        if (username == null || password == null) {
            throw new RuntimeException("username or password is null");
        }
        Optional<Admin> adminCredentialsOpt = FileServices.getAdminCredentials();
        if (adminCredentialsOpt.isEmpty()) {
            return false;
        }
        Admin adminCredentials = adminCredentialsOpt.get();
        String fileUsername = adminCredentials.getUsername();
        String hashedPassword = PasswordHashing.hashPassword(password);  //to compare hashed passwords
        String filePassword = adminCredentials.getPassword();
        return fileUsername.equals(username) && filePassword.equals(hashedPassword);
    }

    public boolean adminExists() {
        return FileServices.getAdminCredentials().isPresent();
    }

    public boolean isCustomerExists(Customer customer) {
        if (customer == null || customer.getAccountNumber() == null) {
            return false;
        }
        String path = FileServices.getUserJsonPath("customers");
        File jsonFile = new File(path);
        if (jsonFile.exists()) {
            JSONArray jsonArray = FileServices.readJsonArrayFile(jsonFile);
            if (jsonArray != null) {
                for (Object obj : jsonArray) {
                    JSONObject userObject = (JSONObject) obj;
                    String accountNumber = (String) userObject.get("accountNumber");
                    if (accountNumber != null && accountNumber.equals(customer.getAccountNumber())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}

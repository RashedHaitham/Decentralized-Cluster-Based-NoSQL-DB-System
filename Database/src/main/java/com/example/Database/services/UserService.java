package com.example.Database.services;

import com.example.Database.file.FileService;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.stereotype.Service;
import java.io.File;
import java.util.Iterator;
import java.util.Map;

@Service
public class UserService {

    public String addCustomer(String accountNumber, String password) {
        String filePath = FileService.getUserJsonPath("customers");
        if (!FileService.isFileExists(filePath)) {
            FileService.writeJsonArrayFile(new File(filePath).toPath(), new JSONArray());
        }
        JSONArray customersArray = FileService.readJsonArrayFile(new File(filePath));
        if (customersArray == null) {
            return "Error reading the file. in database " + filePath;
        }
        for (Object userObj : customersArray) {
            JSONObject customer = (JSONObject) userObj;
            if (customer.get("accountNumber").equals(accountNumber)) {
                return "Customer already exists";
            }
        }
        JSONObject newCustomer = new JSONObject();
        newCustomer.put("accountNumber", accountNumber);
        newCustomer.put("password", password); //hashing the password
        customersArray.add(newCustomer);
        FileService.writeJsonArrayFile(new File(filePath).toPath(), customersArray);
        return "Customer added successfully";
    }

    public String deleteCustomer(String accountNumber) {
        String filePath = FileService.getUserJsonPath("customers");
        if (!FileService.isFileExists(filePath)) {
            return "Customer file not found";
        }
        JSONArray customersArray = FileService.readJsonArrayFile(new File(filePath));
        if (customersArray == null) {
            return "Error reading the customer file";
        }
        boolean found = false;
        Iterator<JSONObject> iterator = customersArray.iterator();
        while (iterator.hasNext()) {
            JSONObject customer = iterator.next();
            if (customer.get("accountNumber").equals(accountNumber)) {
                iterator.remove();
                found = true;
                break;
            }
        }
        if (!found) {
            return "Customer not found";
        }
        FileService.writeJsonArrayFile(new File(filePath).toPath(), customersArray);
        return "Customer deleted successfully";
    }

    public String addAdmin(String username, String password) {
        String filePath = FileService.getUserJsonPath("admin");
        JSONObject newAdmin;
        if (!FileService.isFileExists(filePath)) {
            newAdmin = new JSONObject();
            FileService.writeJsonObjectFile(new File(filePath), newAdmin);
        } else {
            newAdmin = FileService.readJsonObjectFile(new File(filePath));
            if (newAdmin == null) {
                return "Error reading the file. in database " + filePath;
            }
        }
        if (newAdmin.containsKey("username")) {
            if (newAdmin.get("username").equals(username)) {
                return "admin already exists";
            }
        }
        newAdmin.put("username", username);
        newAdmin.put("password", password);
        FileService.writeJsonObjectFile(new File(filePath), newAdmin);
        return "admin added successfully";
    }

    public String updateCustomer(String accountNumber,String password) {
        String filePath = FileService.getUserJsonPath("customers");
        if (!FileService.isFileExists(filePath)) {
            return "Customer file not found";
        }

        JSONArray customersArray = FileService.readJsonArrayFile(new File(filePath));
        if (customersArray == null) {
            return "Error reading the customer file";
        }

        boolean found = false;
        for (int i = 0; i < customersArray.size(); i++) {
            JSONObject customer = (JSONObject) customersArray.get(i);
            if (customer.get("accountNumber").equals(accountNumber)) {
                // Apply updates
                //customer.put("accountNumber", accountNumber);
                customer.put("password", password);
                found = true;
                break;
            }
        }

        if (!found) {
            return "Customer not found";
        }

        FileService.writeJsonArrayFile(new File(filePath).toPath(), customersArray);
        return "Customer updated successfully";
    }

}
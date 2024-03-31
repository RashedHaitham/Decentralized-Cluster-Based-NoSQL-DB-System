package com.example.Bootstrapper.Controller;

import com.example.Bootstrapper.model.Admin;
import com.example.Bootstrapper.model.Customer;
import com.example.Bootstrapper.services.AuthenticationService;
import com.example.Bootstrapper.services.UserService;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bootstrapper")
public class UserController {

    private final UserService userService;

    private final AuthenticationService authenticationService;

    @Autowired
    public UserController(UserService userService, AuthenticationService authenticationService){
        this.userService = userService;
        this.authenticationService = authenticationService;
    }

    @PostMapping("/add/customer")
    public String addCustomer(@RequestHeader("accountNumber") String accountNumber,
                          @RequestHeader("password") String password,
                          @RequestHeader("adminUsername") String adminUsername,
                          @RequestHeader("adminPassword") String adminPassword) {
        if(!authenticationService.isAdmin(adminUsername, adminPassword)){
            return "User is not authorized";
        }
        System.out.println("Received request to register a new customer with account number: " + accountNumber);
        Customer customer = new Customer(accountNumber, password);
        if (authenticationService.isCustomerExists(customer)) {
            return "Customer already exists";
        }
        userService.addCustomer(customer);
        return "Customer has been added successfully";
    }

    @DeleteMapping ("/delete/customer")
    public String deleteCustomer(@RequestHeader("accountNumber") String accountNumber,
                                 @RequestHeader("adminUsername") String adminUsername,
                                 @RequestHeader("adminPassword") String adminPassword) {

        if(!authenticationService.isAdmin(adminUsername, adminPassword)){
            return "User is not authorized";
        }

        System.out.println("Received request to delete the customer with account number: " + accountNumber);
        userService.deleteCustomer(accountNumber);
        return "customer has been deleted successfully";
    }

    @PutMapping("/update/customer")
    public String updateCustomer(@RequestHeader("accountNumber") String accountNumber,
                                 @RequestHeader("adminUsername") String adminUsername,
                                 @RequestHeader("adminPassword") String adminPassword,
                                 @RequestHeader("password") String password
                                 ) {

        if(!authenticationService.isAdmin(adminUsername, adminPassword)){
            return "User is not authorized";
        }

        System.out.println("Received request to update the customer with account number: " + accountNumber);
        userService.updateCustomer(accountNumber,password);
        return "customer has been updated successfully";
    }


    @PostMapping("/add/admin")
    public String addAdmin(@RequestHeader("username") String username,
                           @RequestHeader("password") String password) {
        System.out.println("Received request to register the admin with username: " + username);
        if(authenticationService.adminExists()){
            return "Admin already exists";
        }
        Admin admin = new Admin(username, password);
        userService.addAdmin(admin);
        return "admin has been added successfully";
    }
}
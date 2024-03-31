package com.example.Database.controllers;

import com.example.Database.model.ApiResponse;
import com.example.Database.services.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthenticationController {

    AuthenticationService authenticationService;

    @Autowired
    public AuthenticationController(AuthenticationService authenticationService){
        this.authenticationService = authenticationService;
    }

    @GetMapping("/check/admin")
    public ResponseEntity<String> checkAdminCredentials(@RequestHeader("username") String username,
                                                        @RequestHeader("password") String password) {
        ApiResponse response = authenticationService.verifyAdminCredentials(username, password);
        return ResponseEntity.status(response.getStatus()).body(response.getMessage());
    }

    @GetMapping("/check/customer")
    public ResponseEntity<String> checkCustomerCredentials(@RequestHeader("accountNumber") String accountNumber,
                                                           @RequestHeader("password") String password) {
        ApiResponse response = authenticationService.verifyCustomerCredentials(accountNumber, password);
        return ResponseEntity.status(response.getStatus()).body(response.getMessage());
    }
}
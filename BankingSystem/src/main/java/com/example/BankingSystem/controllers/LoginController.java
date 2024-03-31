package com.example.BankingSystem.controllers;

import com.example.BankingSystem.Model.Admin;
import com.example.BankingSystem.Model.Customer;
import com.example.BankingSystem.enums.Role;
import com.example.BankingSystem.services.AuthenticationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {
    private final AuthenticationService authenticateService;

    @Autowired
    public LoginController(AuthenticationService authenticateService){
        this.authenticateService = authenticateService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login-page";
    }

    @PostMapping("/login")
    public String login(@RequestParam(value = "user") String username,
                        @RequestParam("password") String password,
                        Model model,
                        HttpSession httpSession) {

        ResponseEntity<String> responseEntity;

        responseEntity = authenticateService.checkAdmin(username, password);
        HttpStatus responseStatus = (HttpStatus) responseEntity.getStatusCode();
        if (responseStatus == HttpStatus.OK) {
            Admin admin = new Admin(username, password);
            httpSession.setAttribute("login", admin);
            return "admin-dashboard";
        }
        else {
            responseEntity = authenticateService.checkCustomer(username, password);
            responseStatus = (HttpStatus) responseEntity.getStatusCode();
            if(responseStatus == HttpStatus.OK) {
                Customer customer = new Customer(Long.parseLong(username), password);
                httpSession.setAttribute("login", customer);
                return "redirect:/customer-dashboard/banking-system/";
            }
        }
        String responseBody = responseEntity.getBody();
        model.addAttribute("result", responseBody);
        return "login-page";
    }
}
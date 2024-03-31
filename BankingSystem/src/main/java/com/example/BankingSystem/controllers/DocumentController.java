package com.example.BankingSystem.controllers;

import com.example.BankingSystem.Model.Admin;
import com.example.BankingSystem.Model.BankAccount;
import com.example.BankingSystem.services.DocumentService;
import com.example.BankingSystem.services.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin-dashboard/banking-system")
public class DocumentController {
    private final DocumentService documentService;
    private final UserService userService;

    @Autowired
    public DocumentController(DocumentService documentService, UserService userService){
        this.documentService = documentService;
        this.userService = userService;
    }

    @PostMapping("/createAccount")
    public ResponseEntity<?> createAccount(@RequestParam("db_name") String dbName,
                                         @RequestParam("collection_name") String collectionName,
                                         @ModelAttribute("bankAccount") BankAccount bankAccount,
                                         HttpSession session) {
        Admin login = (Admin) session.getAttribute("login");
        if (login == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).header("Location", "/login-page").body("login-page");
        }
        ResponseEntity<String> responseEntity = documentService.createAccount(dbName, collectionName, bankAccount, session);
        HttpStatus status = (HttpStatus) responseEntity.getStatusCode();
        String message = responseEntity.getBody();
        if (status == HttpStatus.CREATED) {
            List<BankAccount> accounts = documentService.readAccounts(dbName, collectionName, session);
            //add the customer to the bootstrapper after creating their account
            userService.addCustomer(bankAccount.getAccountNumber(), bankAccount.getPassword(), session);
            return ResponseEntity.status(status).body(accounts);
        }else if (status == HttpStatus.CONFLICT) {
            return ResponseEntity.status(status).body(message);
        }else {
            return ResponseEntity.status(status).body(message);
        }
    }

    @GetMapping("/readAccounts")
    public ResponseEntity<?> readAccounts(@RequestParam("db_name") String dbName,
                                          @RequestParam("collection_name") String collectionName,
                                          HttpSession session) {
        Admin login = (Admin) session.getAttribute("login");
        if (login == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).header("Location", "/login-page").body("login-page");
        }
        List<BankAccount> accounts = documentService.readAccounts(dbName, collectionName, session);
        if (!accounts.isEmpty()) {
            return ResponseEntity.status(HttpStatus.OK).body(accounts);
        } else {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body("No accounts found.");
        }
    }

    @PutMapping("/updateDocument")
    public ResponseEntity<?> updateDocumentProperty(
            @RequestParam("db_name") String dbName,
            @RequestParam("collection_name") String collectionName,
            @RequestParam("doc_id") String documentId,
            @ModelAttribute("bankAccount") BankAccount bankAccount,
            HttpSession session) {

        Admin login = (Admin) session.getAttribute("login");
        if (login == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).header("Location", "/login-page").body("login-page");
        }
        ResponseEntity<String> responseEntity = documentService.
                updateDocument(dbName, collectionName,documentId, bankAccount , session);
        HttpStatus status = (HttpStatus) responseEntity.getStatusCode();
        String message = responseEntity.getBody();
        if (status == HttpStatus.ACCEPTED){
            List<BankAccount> accounts = documentService.readAccounts(dbName, collectionName, session);
            if (!bankAccount.getPassword().isEmpty()) {
                userService.updateCustomer(bankAccount.getAccountNumber(), bankAccount.getPassword(), session);
            }
            return ResponseEntity.status(status).body(accounts);
        }else if(status == HttpStatus.NOT_FOUND) {
            return ResponseEntity.status(status).body(message);
        }else {
            return ResponseEntity.status(status).body(message);
        }
    }

    @DeleteMapping("/deleteAccount")
    public ResponseEntity<?> deleteAccount(@RequestParam("db_name") String dbName,
                                           @RequestParam("collection_name") String collectionName,
                                           @RequestParam("doc_id") String documentId,
                                           HttpSession session) {
        Admin login = (Admin) session.getAttribute("login");
        if (login == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).header("Location", "/login-page").body("login-page");
        }
        ResponseEntity<String> responseEntity = documentService.deleteAccount(dbName, collectionName, documentId, session);
        HttpStatus status = (HttpStatus) responseEntity.getStatusCode();
        String message = responseEntity.getBody();
        if (status == HttpStatus.ACCEPTED){
            List<BankAccount> accounts = documentService.readAccounts(dbName, collectionName, session);
            String customerToDelete = message;
            userService.deleteCustomer(customerToDelete, session); //delete customer from the bootstrapper and worker nodes
            return ResponseEntity.status(status).body(accounts);
        }else if(status == HttpStatus.NOT_FOUND) {
            return ResponseEntity.status(status).body(message);
        }else {
            return ResponseEntity.status(status).body(message);
        }
    }
}
package com.example.Database.controllers;

import com.example.Database.model.AccountReference;
import com.example.Database.model.ApiResponse;
import com.example.Database.query.QueryManager;
import com.example.Database.services.AccountDirectoryService;
import com.example.Database.services.AuthenticationService;
import com.example.Database.services.database.DocumentService;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;


@RestController
@RequestMapping("/api")
public class CustomerController {

    private final AccountDirectoryService accountDirectoryService;
    AuthenticationService authenticationService;
    private final QueryManager queryManager;
    private final DocumentService documentService;

    @Autowired
    public CustomerController(AccountDirectoryService accountDirectoryService, QueryManager queryManager, DocumentService documentService,AuthenticationService authenticationService){
        this.accountDirectoryService = accountDirectoryService;
        this.queryManager = queryManager;
        this.documentService = documentService;
        this.authenticationService=authenticationService;
    }

    @PostMapping("/deposit")
    public ResponseEntity<String> deposit(@RequestHeader("accountNumber") String accountNumber,
                                          @RequestBody JSONObject request) {
        double amountToDeposit = Double.parseDouble(request.get("amount").toString());
        System.out.println(amountToDeposit + " amount being deposited");
        AccountReference accountReference = accountDirectoryService.getAccountLocation(accountNumber);
        String dbName = accountReference.getDatabaseName();
        String collectionName = accountReference.getCollectionName();
        String documentId = accountReference.getDocumentId();
        String propertyName = "balance";

        ApiResponse response = queryManager.searchForProperty( //first search for the existing balance
                dbName,
                collectionName,
                documentId,
                propertyName
        );

        //response.getMessage has the original balance value
        double newBalance = Double.parseDouble(response.getMessage()) + amountToDeposit; //update the balance after withdrawing

        response = queryManager.updateDocumentProperty(
                dbName,
                collectionName,
                documentId,
                propertyName,
                newBalance,
                "false"
        );

        response.setMessage(String.valueOf(newBalance));
        if (response.getStatus() == HttpStatus.ACCEPTED) {
            return ResponseEntity.status(response.getStatus()).body(response.getMessage()); // Return the new balance
        } else {
            return ResponseEntity.status(response.getStatus()).body("Deposit failed");
        }
    }

    @PostMapping("/withdraw")
    public ResponseEntity<String> withdraw(@RequestHeader("accountNumber") String accountNumber,
                                           @RequestBody JSONObject request) {
        double amountToWithdraw = Double.parseDouble(request.get("amount").toString());
        AccountReference accountReference = accountDirectoryService.getAccountLocation(accountNumber);

        String dbName = accountReference.getDatabaseName();
        String collectionName = accountReference.getCollectionName();
        String documentId = accountReference.getDocumentId();
        String propertyName = "balance";

        ApiResponse response = queryManager.searchForProperty( //first search for the existing balance
                dbName,
                collectionName,
                documentId,
                propertyName
        );

        //response.getMessage has the original balance value
        double newBalance = Double.parseDouble(response.getMessage()) - amountToWithdraw;  //update the balance after withdrawing

        response = queryManager.updateDocumentProperty(
                dbName,
                collectionName,
                documentId,
                propertyName,
                newBalance,
                "false"
        );

        response.setMessage(String.valueOf(newBalance));
        if (response.getStatus() == HttpStatus.ACCEPTED) {
            return ResponseEntity.status(response.getStatus()).body(response.getMessage()); // Return the new balance
        } else {
            return ResponseEntity.status(response.getStatus()).body("Withdrawal failed");
        }
    }

    @PostMapping("/transfer")
    public ResponseEntity<String> transfer(@RequestHeader("accountNumber") String senderAccountNumber,
                                           @RequestBody JSONObject request) {

        String recipientAccountNumber = request.get("transferTo").toString();
        AccountReference accountReference = accountDirectoryService.getAccountLocation(senderAccountNumber);

        if (!documentService.checkAccountNumberExists(accountReference.getCollectionName(),recipientAccountNumber)){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("no such ID.");
        }
        ResponseEntity<String> withdrawResponse = withdraw(senderAccountNumber, request);

        if (withdrawResponse.getStatusCode() != HttpStatus.ACCEPTED) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Transfer failed: Unable to withdraw funds");
        }

        ResponseEntity<String> depositResponse = deposit(recipientAccountNumber, request);
        if (depositResponse.getStatusCode() != HttpStatus.ACCEPTED) {
            // Rollback the withdrawal from the sender's account
            deposit(senderAccountNumber, request);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Transfer failed: Unable to deposit funds");
        }
        // Transfer successful
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(withdrawResponse.getBody());
    }


    @PostMapping("/close")
    public ResponseEntity<String> close(@RequestHeader("accountNumber") String accountNumber,
                                           @RequestBody JSONObject request) {
        String pin=request.get("pin").toString();
        String user=request.get("user").toString();
        ApiResponse response=authenticationService.verifyCustomerCredentials(user,pin);
        if (response.getStatus()!=HttpStatus.OK)
            return ResponseEntity.status(response.getStatus()).body("Wrong Credentials");
        AccountReference accountReference = accountDirectoryService.getAccountLocation(accountNumber);
        String dbName = accountReference.getDatabaseName();
        String collectionName = accountReference.getCollectionName();
        String documentId = accountReference.getDocumentId();
        String propertyName = "status";
        boolean newStatus = false;

        response = queryManager.updateDocumentProperty(
                dbName,
                collectionName,
                documentId,
                propertyName,
                newStatus,
                "false"
        );

        if (response.getStatus() == HttpStatus.ACCEPTED) {
            return ResponseEntity.status(response.getStatus()).body("account closed");
        } else {
            return ResponseEntity.status(response.getStatus()).body("close failed");
        }
    }
}
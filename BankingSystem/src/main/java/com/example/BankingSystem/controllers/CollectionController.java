package com.example.BankingSystem.controllers;

import com.example.BankingSystem.Model.Admin;
import com.example.BankingSystem.services.CollectionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/admin-dashboard/banking-system")
public class CollectionController {

    private final CollectionService collectionService;

    @Autowired
    public CollectionController(CollectionService collectionService){
        this.collectionService = collectionService;
    }

    @PostMapping("/createCol")
    public ResponseEntity<?> createCollection(@RequestParam("db_name") String dbName,
                                              @RequestParam("collection_name") String collectionName,
                                              HttpSession session) {
        Admin login = (Admin) session.getAttribute("login");
        if (login == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).header("Location", "/login-page").body("login-page");
        }
        ResponseEntity<String> response = collectionService.createCollection(dbName, collectionName, session);
        HttpStatus status = (HttpStatus) response.getStatusCode();
        String message = response.getBody();
        if (status == HttpStatus.CREATED) {
            List<String> allCollections = collectionService.getAllCollections(dbName, session);
            return ResponseEntity.status(status).body(allCollections);
        } else if (status == HttpStatus.CONFLICT) {
            return ResponseEntity.status(status).body(message);
        } else {
            return ResponseEntity.status(status).body(message);
        }
    }

    @DeleteMapping("/deleteCol")
    public ResponseEntity<?> deleteCollection(@RequestParam("db_name") String dbName,
                                              @RequestParam("collection_name") String collectionName,
                                              HttpSession session) {
        Admin login = (Admin) session.getAttribute("login");
        if (login == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).header("Location", "/login-page").body("login-page");
        }
        ResponseEntity<String> responseEntity = collectionService.deleteCollection(dbName, collectionName, session);
        HttpStatus status = (HttpStatus) responseEntity.getStatusCode();
        String message = responseEntity.getBody();
        if (status == HttpStatus.ACCEPTED) {
            List<String> allCollections = collectionService.getAllCollections(dbName, session);
            return ResponseEntity.status(status).body(allCollections);
        } else if (status == HttpStatus.NOT_FOUND){
            return ResponseEntity.status(status).body(message);
        }else{
            return ResponseEntity.status(status).body(message);
        }
    }

    @GetMapping("/fetchExistingCollections")
    @ResponseBody
    public ResponseEntity<List<String>> fetchExistingCollections(@RequestParam("db_name") String dbName,
                                                                 HttpSession session) {
        Admin login = (Admin) session.getAttribute("login");
        if (login == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).header("Location", "/login-page").body(Collections.emptyList());
        }
        List<String> allCollections = collectionService.getAllCollections(dbName, session);
        if (allCollections.isEmpty()) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.status(HttpStatus.OK).body(allCollections);
        }
    }
}
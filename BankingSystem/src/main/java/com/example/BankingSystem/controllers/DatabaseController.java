package com.example.BankingSystem.controllers;

import com.example.BankingSystem.Model.Admin;
import com.example.BankingSystem.services.DatabaseService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/admin-dashboard/banking-system")
public class DatabaseController {

    private final DatabaseService databaseService;

    @Autowired
    public DatabaseController(DatabaseService databaseService){
        this.databaseService = databaseService;
    }

    @PostMapping("/createDB")
    public ResponseEntity<?> createDb(@RequestParam("db_name") String dbName, HttpSession session) {
        Admin login = (Admin) session.getAttribute("login");
        if (login == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).header("Location", "/login-page").body("login-page");
        }
        ResponseEntity<String> response = databaseService.createDatabase(dbName, session);
        HttpStatus status = (HttpStatus) response.getStatusCode();
        String message = response.getBody();
        if (status == HttpStatus.CREATED) {
            List<String> allDatabases = databaseService.getAllDatabases(session);
            return ResponseEntity.status(status).body(allDatabases);
        } else if (status == HttpStatus.CONFLICT) {
            return ResponseEntity.status(status).body(message);
        } else {
            return ResponseEntity.status(status).body(message);
        }
    }

    @DeleteMapping("/deleteDB")
    public ResponseEntity<?> deleteDatabase(@RequestParam("db_name") String dbName,
                                            HttpSession session) {
        Admin login = (Admin) session.getAttribute("login");
        if (login == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).header("Location", "/login-page").body("login-page");
        }
        ResponseEntity<String> responseEntity = databaseService.deleteDatabase(dbName, session);
        HttpStatus status = (HttpStatus) responseEntity.getStatusCode();
        String message = responseEntity.getBody();
        if (status == HttpStatus.ACCEPTED) {
            List<String> allDatabases = databaseService.getAllDatabases(session);
            return ResponseEntity.status(status).body(allDatabases);
        } else if (status == HttpStatus.NOT_FOUND) {
            return ResponseEntity.status(status).body(message);
        } else {
            return ResponseEntity.status(status).body(message);
        }
    }

    @GetMapping("/fetchExistingDatabases")
    @ResponseBody
    public ResponseEntity<List<String>> fetchExistingDatabases(HttpSession session) {
        Admin login = (Admin) session.getAttribute("login");
        if (login == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).header("Location", "/login-page").body(Collections.emptyList());
        }
        System.out.println(login.getUsername());
        System.out.println(login.getPassword());
        List<String> databaseNames = databaseService.getAllDatabases(session);
        if (databaseNames.isEmpty()) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.status(HttpStatus.OK).body(databaseNames);
        }
    }
}
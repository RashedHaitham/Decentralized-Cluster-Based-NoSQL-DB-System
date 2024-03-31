package com.example.Database.controllers;

import com.example.Database.file.FileService;
import com.example.Database.model.ApiResponse;
import com.example.Database.query.QueryManager;
import com.example.Database.services.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class DatabaseController {

    private final AuthenticationService authenticationService;
    private final QueryManager queryManager;

    @Autowired
    public DatabaseController(AuthenticationService authenticationService, QueryManager queryManager){
        this.authenticationService = authenticationService;
        this.queryManager = queryManager;
    }

    @PostMapping("/createDB/{db_name}")
    public ResponseEntity<String> createDatabase(@PathVariable("db_name") String dbName,
                                                 @RequestHeader(value = "X-Broadcast", required = false, defaultValue = "false") String isBroadcasted,
                                                 @RequestHeader("username") String username,
                                                 @RequestHeader("password") String password) {

        if(!authenticationService.isAdmin(username, password)){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User is not authorized");
        }
        FileService.setDatabaseDirectory(dbName);
        ApiResponse response = queryManager.createDatabase(dbName, isBroadcasted);
        return ResponseEntity.status(response.getStatus()).body(response.getMessage());
    }

    @DeleteMapping("/deleteDB/{db_name}")
    public ResponseEntity<String> deleteDatabase(@PathVariable("db_name") String dbName,
                                                 @RequestHeader(value = "X-Broadcast", required = false, defaultValue = "false") String isBroadcasted,
                                                 @RequestHeader("username") String username,
                                                 @RequestHeader("password") String password) {
        if(!authenticationService.isAdmin(username, password)){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User is not authorized");
        }
        FileService.setDatabaseDirectory(dbName);
        ApiResponse response = queryManager.deleteDatabase(dbName, isBroadcasted);
        return ResponseEntity.status(response.getStatus()).body(response.getMessage());
    }

    @GetMapping("/fetchExistingDatabases")
    public ResponseEntity<List<String>> fetchExistingDatabases(
            @RequestHeader("username") String username,
            @RequestHeader("password") String password) {
        if (!authenticationService.isAdmin(username, password)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        List<String> databases = queryManager.readDatabases();
        return new ResponseEntity<>(databases, HttpStatus.OK);
    }
}
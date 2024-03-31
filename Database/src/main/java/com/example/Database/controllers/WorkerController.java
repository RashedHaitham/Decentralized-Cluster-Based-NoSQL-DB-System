package com.example.Database.controllers;

import com.example.Database.affinity.AffinityManager;
import com.example.Database.services.AuthenticationService;
import com.example.Database.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class WorkerController {

    private final AuthenticationService authenticationService;

    private final AffinityManager affinityManager;

    private final UserService userService;

    @Autowired
    public WorkerController(AuthenticationService authenticationService, UserService userService, AffinityManager affinityManager){
        this.authenticationService = authenticationService;
        this.userService = userService;
        this.affinityManager = affinityManager;
    }

    @GetMapping("/setCurrentWorkerName/{worker_name}")
    public ResponseEntity<String> setCurrentWorkerName(@PathVariable("worker_name") String workerName) {
        System.out.println("Received request to set worker name to: " + workerName);
        try {
            affinityManager.setCurrentWorkerPort(workerName);
            return ResponseEntity.status(HttpStatus.OK).body("The current worker name is set to: " + workerName);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error setting worker name: " + e.getMessage());
        }
    }
}
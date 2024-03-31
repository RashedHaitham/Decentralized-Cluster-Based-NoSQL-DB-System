package com.example.Bootstrapper.services.network;

import com.example.Bootstrapper.loadbalancer.LoadBalancer;
import com.example.Bootstrapper.model.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class NetworkService {


    private final NodesService nodesService;
    private final LoadBalancer loadBalancer;


    @Autowired
    public NetworkService(NodesService nodesService, LoadBalancer loadBalancer){
        this.nodesService = nodesService;
        this.loadBalancer = loadBalancer;
    }


    public void run() {
        createNetwork();
        checkClusterStatus();
        loadBalancer.balanceExistingUsers();
    }

    private void createNetwork() {
        for (int i = 1; i <= 4; i++) {
            Node node = new Node();
            node.setNodeNumber(i);
            node.setNodeIP("172.16.1.10" + i);
            nodesService.addNode(node);
        }
        setUpWorkersNames();
    }

    private void setUpWorkersNames() {
        for (Node node : nodesService.getNodes()) {
            try {
                String workerName = "worker" + node.getNodeNumber();
                String url = "http://" + node.getNodeIP() + ":9000/api/setCurrentWorkerName/" + workerName;
                HttpHeaders headers = new HttpHeaders();
                HttpEntity<String> requestEntity = new HttpEntity<>(headers);
                RestTemplate restTemplate = new RestTemplate();
                System.out.println("sending request to add worker with " + node.getNodeIP());
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
                if (response.getStatusCode() == HttpStatus.OK) {
                    System.out.println("Successfully set worker name for node: " + node.getNodeIP());
                    node.setActive(true);
                } else {
                    System.out.println("Failed to set worker name for node: " + node.getNodeIP() + ". HTTP Status: " + response.getStatusCode());
                }
            } catch (Exception e) {
                System.out.println("Error setting up worker name for node: " + node.getNodeIP());
                e.printStackTrace();
            }
        }
    }

    private void checkClusterStatus() {
        for (Node node : nodesService.getNodes()) {
            if (node.isActive()) {
                System.out.println("Node " + node.getNodeNumber() + " is active.");
            } else {
                System.out.println("Node " + node.getNodeNumber() + " is not active.");
            }
        }
    }
}
package com.example.Bootstrapper.services;

import com.example.Bootstrapper.loadbalancer.LoadBalancer;
import com.example.Bootstrapper.model.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WorkerService {

    private final LoadBalancer loadBalancer;

    @Autowired
    public WorkerService(LoadBalancer loadBalancer){
        this.loadBalancer = loadBalancer;
    }

    public String getWorker(String identity){
        Node node = loadBalancer.getUserNode(identity);
        if (node != null) {
            return String.valueOf(node.getNodeNumber());
        } else {
            return "Customer not found on any worker";
        }
    }
}

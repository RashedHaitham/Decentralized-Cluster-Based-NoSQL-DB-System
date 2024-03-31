package com.example.Bootstrapper.loadbalancer;

import com.example.Bootstrapper.File.FileServices;
import com.example.Bootstrapper.model.Node;
import com.example.Bootstrapper.services.network.NodesService;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LoadBalancer {

    private final NodesService nodesService;

    private final Map<Node, List<String>> nodeUsers;
    private int nextNodeIndex;

    @Autowired
    public LoadBalancer(NodesService nodesService){
        this.nodesService = nodesService;
        nodeUsers = new HashMap<>();
    }

    public void balanceExistingUsers() {
        nextNodeIndex = 0;
        System.out.println("balancing existing users..................");
        File customersFile = new File(FileServices.getUserJsonPath("customers"));
        if (FileServices.isFileExists(customersFile.getPath())) {
            JSONArray usersArray = FileServices.readJsonArrayFile(customersFile);
            if (usersArray != null) {
                for (Object obj : usersArray) {
                    JSONObject userJson = (JSONObject) obj;
                    String accountNumber = (String) userJson.get("accountNumber");
                    this.assignUserToNextNode(accountNumber);
                }
            }
        } else {
            System.out.println("No existing users found...");
        }
    }


    public Node assignUserToNextNode(String identity) {
        Node node = getNextNode();
        if (!nodeUsers.containsKey(node)) {
            nodeUsers.put(node, new ArrayList<>());
        }
        nodeUsers.get(node).add(identity);
        System.out.println("user is assigned to node " + node.getNodeNumber() + " and the ip address is " + node.getNodeIP());
        return node;
    }


    public synchronized Node getNextNode() {
        if (nodesService.getNodes().isEmpty()) {
            throw new IllegalStateException("No nodes available for balancing");
        }
        Node nextNode = nodesService.getNodes().get(nextNodeIndex);
        updateNextNodeIndex();
        return nextNode;
    }

    private void updateNextNodeIndex() {
        nextNodeIndex = (nextNodeIndex + 1) % nodesService.getNodes().size();
    }

    public Node getUserNode(String identity) {
        for (Map.Entry<Node, List<String>> entry : nodeUsers.entrySet()) {
            if (entry.getValue().contains(identity)) {
                return entry.getKey();
            }
        }
        return null;
    }
}
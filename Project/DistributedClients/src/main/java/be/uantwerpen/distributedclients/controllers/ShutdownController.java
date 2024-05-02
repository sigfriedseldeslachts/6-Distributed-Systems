package be.uantwerpen.distributedclients.controllers;

import be.uantwerpen.distributedclients.models.Node;
import be.uantwerpen.distributedclients.services.InfoService;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.List;

@RestController
@RequestMapping("shutdown")
public class ShutdownController {
    private InfoService infoservice;

    @PutMapping("")
    public void shutdown(){
        //Get Map of all nodes in naming server
        RestClient client = RestClient.builder()
                .baseUrl("http://" + this.infoservice.getNamingServerAddress() + "/nodes/")
                .defaultHeader("Content-Type", "application/json")
                .build();
        List<Node> allNodes = client.get().retrieve().body(new ParameterizedTypeReference<>() {});

        String nextIdAddress = "";
        String previousIdAddress = "";
        assert allNodes != null;
        for (Node node : allNodes) {
            if (node.hashCode() == this.infoservice.getNextID()) {
                nextIdAddress = node.getSocketAddress();
            }
            if (node.hashCode() == this.infoservice.getPreviousID()) {
                previousIdAddress = node.getSocketAddress();
            }
        }
        //Send NextID to previous node
        client = RestClient.builder()
                .baseUrl("http://" + previousIdAddress + "/next/" + this.infoservice.getNextID())
                .defaultHeader("Content-Type", "application/json")
                .build();
        client.put().retrieve();

        //Send PreviousID to next node
        client = RestClient.builder()
                .baseUrl("http://" + nextIdAddress + "previous/" + this.infoservice.getPreviousID())
                .defaultHeader("Content-Type", "application/json")
                .build();
        client.put().retrieve();

        //Send delete request to naming server
        client = RestClient.builder()
                .baseUrl("http://" + this.infoservice.getNamingServerAddress() + "/nodes/" + this.infoservice.getSelfNode().hashCode())
                .defaultHeader("Content-Type", "application/json")
                .build();
        client.delete().retrieve();
    }
}

package be.uantwerpen.distributedclients.controllers;

import be.uantwerpen.distributedclients.models.Node;
import be.uantwerpen.distributedclients.services.InfoService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("shutdown")
public class ShutdownController {
    private InfoService infoservice;

    @PutMapping("")
    public void shutdown(){
        //Get Map of all nodes in naming server
        RestClient client = RestClient.builder()
                .baseUrl("http://" + this.infoservice.getNamingserverAddress() + "/nodes/")
                .defaultHeader("Content-Type", "application/json")
                .build();
        List<Node> nodes = client.get().retrieve().body(new ParameterizedTypeReference<>() {});

        //Send NextID to previous node
        client = RestClient.builder()
                .baseUrl("http://" + nodes.get(this.infoservice.getNextID()).getSocketAddress())
                .defaultHeader("Content-Type", "application/json")
                .build();
        client.post().retrieve();

        //Send PreviousID to next node
        client = RestClient.builder()
                .baseUrl("http://" + nodes.get(this.infoservice.getPreviousID()).getSocketAddress())
                .defaultHeader("Content-Type", "application/json")
                .build();
        client.post().retrieve();

        //Send delete request to naming server
        client = RestClient.builder()
                .baseUrl("http://" + this.infoservice.getNamingserverAddress() + "/nodes/" + this.infoservice.getNode().hashCode())
                .defaultHeader("Content-Type", "application/json")
                .build();
        client.delete().retrieve();
    }
}

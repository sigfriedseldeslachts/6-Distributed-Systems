package be.uantwerpen.namingserver.controllers;

import be.uantwerpen.namingserver.models.Node;
import be.uantwerpen.namingserver.services.NodeService;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/nodes")
public class NodeController {

    private final NodeService nodeService;

    public NodeController(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    @GetMapping("")
    public List<Node> getNodes() {
        return this.nodeService.getAllNodes();
    }

    @DeleteMapping("{hashcode}")
    public void deleteNode(@PathVariable int hashcode) {
        this.nodeService.deleteNode(hashcode);
    }

    @GetMapping("{nodeName}")
    public Map getNode(@PathVariable String nodeName) {
        Node node = nodeService.getNode(nodeName);
        if (node == null) {
            return null;
        }

        // Do an HTTP request to the node
        RestClient customClient = RestClient.builder()
                .baseUrl("http://" + node.getSocketAddress() + "/nodes")
                .build();

        ResponseEntity<Map> response = customClient.get()
                .retrieve()
                .toEntity(new ParameterizedTypeReference<>() {});

        return response.getBody();
    }
}

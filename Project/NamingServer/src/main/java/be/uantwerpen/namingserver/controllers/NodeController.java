package be.uantwerpen.namingserver.controllers;

import be.uantwerpen.namingserver.models.Node;
import be.uantwerpen.namingserver.services.NodeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PostMapping("")
    public Node addNode(@RequestBody Node node) {
        this.nodeService.addNode(node);
        return node;
    }

}

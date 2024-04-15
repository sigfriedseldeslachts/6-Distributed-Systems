package be.uantwerpen.namingserver.controllers;

import be.uantwerpen.namingserver.models.Node;
import be.uantwerpen.namingserver.services.NodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/files")
public class FileController {

    @Autowired
    private NodeService nodeService;

    @GetMapping("{filename}")
    public Node show(@PathVariable String filename)
    {
        if (filename == null) { // null == "null" is true
            throw new IllegalArgumentException("Filename cannot be null");
        }

        Node node = nodeService.getNodeToStoreFileOn(filename);

        System.out.println(node);

        return node;
    }

}

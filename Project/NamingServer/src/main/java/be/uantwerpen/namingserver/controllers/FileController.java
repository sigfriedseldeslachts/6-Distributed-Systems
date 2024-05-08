package be.uantwerpen.namingserver.controllers;

import be.uantwerpen.namingserver.models.Node;
import be.uantwerpen.namingserver.services.NodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @PostMapping("replication")
    public Map<Integer, Integer> replicate(@RequestBody List<Integer> fileHashes)
    {
        Map<Integer, Integer> nodesToReplicateFilesOn = new HashMap<>();
        if (fileHashes == null || fileHashes.isEmpty()) {
            return nodesToReplicateFilesOn;
        }

        for (int fileHash : fileHashes) {
            Node node = nodeService.getNodeToStoreFileOn(fileHash);
            nodesToReplicateFilesOn.put(fileHash, node.hashCode());
        }

        return nodesToReplicateFilesOn;
    }

}

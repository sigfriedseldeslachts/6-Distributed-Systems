package be.uantwerpen.distributedclients.controllers;

import be.uantwerpen.distributedclients.services.FileService;
import be.uantwerpen.distributedclients.services.InfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("nodes")
public class NodeController {

    private final Logger logger = LoggerFactory.getLogger(NodeController.class);
    private final InfoService infoservice;
    private final FileService fileService;

    public NodeController(InfoService infoservice, FileService fileService) {
        this.infoservice = infoservice;
        this.fileService = fileService;
    }

    @GetMapping
    public Object ShowNodeInfo() {
        HashMap<String, Object> map = new HashMap<>();

        map.put("previous_id", infoservice.getPreviousID());
        map.put("next_id", infoservice.getNextID());
        map.put("own_hash", infoservice.getSelfNode().hashCode());
        map.put("own_node", infoservice.getSelfNode());
        map.put("all_nodes", infoservice.getNodes());
        map.put("naming_server", infoservice.getNamingServerAddress());
        map.put("local_files", fileService.getLocalFileList());
        map.put("replicated_files", fileService.getReplicatedFileList());

        return map;
    }

    @PatchMapping("server")
    public void UpdateNamingServer(@RequestBody Map<String, String> body) {
        // Check if the address and port are valid
        if (!body.containsKey("address") || !body.containsKey("port")) {
            throw new IllegalArgumentException("Invalid body");
        }

        InetSocketAddress address = InetSocketAddress.createUnresolved(body.get("address"), Integer.parseInt(body.get("port")));
        this.infoservice.setNamingServerAddress(address);

        logger.debug("Updating naming server address to: {}", infoservice.getNamingServerAddress());
    }
}

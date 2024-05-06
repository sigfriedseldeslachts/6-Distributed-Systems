package be.uantwerpen.namingserver.services;

import be.uantwerpen.namingserver.models.Node;
import be.uantwerpen.namingserver.utils.HashingFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
public class NodeService {

    private final HashMap<Integer, Node> nodes = new HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Logger logger = LoggerFactory.getLogger(NodeService.class);

    public int getNumberOfNodes() {
        return nodes.size();
    }

    public Node getNode(int hash) {
        return nodes.get(hash);
    }

    public Node getNode(String name) {
        return getNode(HashingFunction.getHashFromString(name));
    }

    public void addNode(Node node) {
        // Check if a node with the same hash already exists
        if (this.nodes.containsKey(node.hashCode())) {
            throw new IllegalArgumentException("Node with the same hash already exists");
        }

        nodes.put(node.hashCode(), node);

        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File("nodes.json"), nodes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void deleteNode(int hashcode) {
        nodes.remove(hashcode);

        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File("nodes.json"), nodes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public Node getNodeToStoreFileOn(String filename) {
        if (this.nodes.isEmpty()) {
            throw new IllegalStateException("No nodes available to store file on");
        }

        int file_hash = HashingFunction.getHashFromString(filename);
        int biggestNode = Integer.MIN_VALUE;
        int smallestDiff = Integer.MAX_VALUE;
        int smallestDiffNode = Integer.MAX_VALUE;

        for (int nodeHash : this.nodes.keySet()) {
            // Check for node with the smallest difference
            if (nodeHash < file_hash) {
                if (file_hash - nodeHash < smallestDiff) {
                    smallestDiff = file_hash - nodeHash;
                    smallestDiffNode = nodeHash;
                }
            }

            // Check for biggest node
            if (nodeHash > biggestNode) {
                biggestNode = nodeHash;
            }
        }

        // If we have no smallestDiffNode it will still equal MAX_VALUE
        if (smallestDiffNode == Integer.MAX_VALUE) {
            return this.nodes.get(biggestNode);
        }

        return this.nodes.get(smallestDiffNode);
    }

    public List<Node> getAllNodes() {
        return new ArrayList<>(this.nodes.values());
    }

    public void updateNode(Node node) {
        Node oldNode = this.nodes.get(node.hashCode());
        oldNode.setLastPing(LocalDateTime.now());

        // We only allow to set the node leaving and not disable it
        // Otherwise if we can "cancel" a shutdown it may cause problems
        if (node.isLeaving()) {
            oldNode.setLeaving();
        }
    }

    /**
     * Remove all nodes that are stale
     */
    @Scheduled(fixedRate = 5000)
    public void removeStaleNodes() {
        logger.info("Removing stale nodes...");
        //update json file
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File("nodes.json"), nodes);
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.nodes.entrySet().removeIf(entry -> {
            if (entry.getValue().isLeaving()) {
                logger.info("Removing leaving node: {}", entry.getValue().getName());
                return true;
            }

            if (entry.getValue().isStale()) {
                logger.info("Removing stale node: {}", entry.getValue().getName());
                return true;
            }

            return false;
        });
    }

}

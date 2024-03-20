package be.uantwerpen.namingserver.services;

import be.uantwerpen.namingserver.models.Node;
import be.uantwerpen.namingserver.utils.HashingFunction;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;

@Service
public class NodeService {

    private final HashMap<Integer, Node> nodes = new HashMap<>();

    public Node getNode(String name) {
        return nodes.get(HashingFunction.getHashFromString(name));
    }

    public Node getNode(int hash) {
        return nodes.get(hash);
    }

    public void addNode(Node node) {
        nodes.put(node.hashCode(), node);
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
        if (smallestDiffNode != Integer.MAX_VALUE) {
            return this.nodes.get(biggestNode);
        }

        return this.nodes.get(smallestDiffNode);
    }

}

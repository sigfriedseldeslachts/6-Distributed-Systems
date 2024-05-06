package be.uantwerpen.distributedclients.services;

import be.uantwerpen.distributedclients.models.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.TreeMap;

@Service
@EnableAsync
public class InfoService {
    private int previousID;
    private int nextID;
    private Node selfNode;
    private final TreeMap<Integer, Node> nodes = new TreeMap<>();
    private final Logger logger = LoggerFactory.getLogger(AnnouncingService.class);

    public Node getSelfNode() {
        return selfNode;
    }

    public void setSelfNode(Node selfNode) {
        this.selfNode = selfNode;
        this.nodes.put(selfNode.hashCode(), selfNode);
    }

    public void addNewNode(Node newNode) {
        this.nodes.put(newNode.hashCode(), newNode);
    }

    public int getPreviousID() {
        return previousID;
    }

    public int getNextID() {
        return nextID;
    }

    public TreeMap<Integer, Node> getNodes() {
        return nodes;
    }

    /**
     * Update the order of the nodes
     */
    public void updateNodeOrder() {
        // Find the index where I am currently at
        int index = 0;
        for (int nodeHash : this.nodes.keySet()) {
            if (nodeHash == this.selfNode.hashCode()) break;
            index++;
        }

        // If we are the only node, we don't need to update the ID's
        if (this.nodes.size() <= 1) {
            this.previousID = this.selfNode.hashCode();
            this.nextID = this.selfNode.hashCode();
            return;
        }

        List<Integer> keys = this.nodes.keySet().stream().toList();

        // If we are the first node we set the previous node to the last node
        if (index == 0) {
            this.previousID = this.nodes.lastKey();
            this.nextID = keys.get(index + 1); // Get the next node (ourselves + 1)
            return;
        }

        // If we are the last node we set the next node to the first node
        if (index == this.nodes.size() - 1) {
            this.nextID = this.nodes.firstKey();
            this.previousID = keys.get(index - 1); // Get the previous node (ourselves - 1)
            return;
        }

        // If we are in the middle we set the previous node to the previous node and the next node to the next node
        this.previousID = keys.get(index - 1);
        this.nextID = keys.get(index + 1);
    }

    /**
     * Remove all nodes that are stale
     */
    @Async
    @Scheduled(fixedRate = 5000)
    public void removeStaleNodes() {
        logger.info("Removing stale nodes...");

        this.nodes.entrySet().removeIf(entry -> {
            // Obviously we don't want to remove ourselves
            if (entry.getValue().hashCode() == this.selfNode.hashCode()) {
                return false;
            }

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

        updateNodeOrder();
    }
}

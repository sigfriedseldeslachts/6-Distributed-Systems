package be.uantwerpen.distributedclients.services;

import be.uantwerpen.distributedclients.models.Node;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Service
public class InfoService {

    private Node node;

    private int previousID;
    private int nextID;
    private int hashOfNewNode;

    private int amountOfNodes;

    public InfoService() throws UnknownHostException {
        this.node = new Node(InetAddress.getLocalHost().getHostName(), InetAddress.getLoopbackAddress());
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public int getPreviousID() {
        return previousID;
    }

    public void setPreviousID(int previousID) {
        this.previousID = previousID;
    }

    public int getNextID() {
        return nextID;
    }

    public void setNextID(int nextID) {
        this.nextID = nextID;
    }

    public int getHashOfNewNode() {
        return hashOfNewNode;
    }

    public void setHashOfNewNode(int hashOfNewNode) {
        this.hashOfNewNode = hashOfNewNode;
    }

    public int getAmountOfNodes() {
        return amountOfNodes;
    }

    public void setAmountOfNodes(int amountOfNodes) {
        this.amountOfNodes = amountOfNodes;
    }

    public void updateID() {
        if (this.amountOfNodes<1) {
            this.previousID = this.node.hashCode();
            this.nextID = this.node.hashCode();
        } else {
            if ((this.node.hashCode() < this.hashOfNewNode) & (this.hashOfNewNode < this.nextID)) {
                this.nextID = this.hashOfNewNode;
                // add http request to send info on current id and next  // ADD BODY
                RestClient client = RestClient.builder()
                        .baseUrl("http://" + node.getAddress() + "/nodes/next")
                        .defaultHeader("Content-Type", "application/json")
                        .build();
                client.post().retrieve();

            }
            if ((this.previousID < this.hashOfNewNode) & (this.hashOfNewNode < this.node.hashCode())) {
                this.previousID = this.hashOfNewNode;
                // add http request to send info on current id and previous
            }
        }
    }



}

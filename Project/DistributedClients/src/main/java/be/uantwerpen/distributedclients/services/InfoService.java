package be.uantwerpen.distributedclients.services;

import be.uantwerpen.distributedclients.models.Node;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Service
public class InfoService {

    private Node node; // Current node

    private int previousID;
    private int nextID;

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

    public int getAmountOfNodes() {
        return amountOfNodes;
    }

    public void setAmountOfNodes(int amountOfNodes) {
        this.amountOfNodes = amountOfNodes;
    }

    public void updateID(Node newNode) {
        if (this.amountOfNodes<=1) {
            this.previousID = newNode.hashCode();
            this.nextID = newNode.hashCode();
        } else {
            if ((this.node.hashCode() < newNode.hashCode()) & (newNode.hashCode() < this.nextID)) {
                this.nextID = newNode.hashCode();
                // add http request to send info on current id and next  // ADD BODY
                RestClient client = RestClient.builder()
                        .baseUrl("http://" + node.getAddress().getHostAddress() + ":8080/nodes/next"
                            + "?currentId=" + this.node.hashCode() + "&nextId=" + this.nextID
                        )
                        .defaultHeader("Content-Type", "application/json")
                        .build();
                client.post().retrieve();

            }
            if ((this.previousID < newNode.hashCode()) & (newNode.hashCode() < this.node.hashCode())) {
                this.previousID = newNode.hashCode();
                // add http request to send info on current id and previous
                RestClient client = RestClient.builder()
                        .baseUrl("http://" + node.getAddress().getHostAddress() + ":8080/nodes/previous"
                                + "?currentId=" + this.node.hashCode() + "&previousId=" + this.previousID
                        )
                        .defaultHeader("Content-Type", "application/json")
                        .build();
                client.post().retrieve();
            }
        }
    }



}

package be.uantwerpen.distributedclients.models;

import be.uantwerpen.distributedclients.utils.HashingFunction;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.net.InetSocketAddress;
import java.time.LocalDateTime;

public class Node {

    private String name;
    private InetSocketAddress socketAddress;
    @JsonIgnore
    private LocalDateTime lastPing = LocalDateTime.now();

    public Node() {}

    public Node(String name, InetSocketAddress address) {
        this.name = name;
        this.socketAddress = address;
    }

    public String getName() {
        return name;
    }

    public String getSocketAddress() {
        return socketAddress.getHostString() + ":" + socketAddress.getPort();
    }

    public void setLastPing(LocalDateTime lastPing) {
        this.lastPing = lastPing;
    }

    public LocalDateTime getLastPing() {
        return lastPing;
    }

    @JsonIgnore
    public boolean isStale() {
        return lastPing.isBefore(LocalDateTime.now().minusSeconds(10));
    }

    @Override
    public int hashCode() {
        return HashingFunction.getHashFromString(name);
    }
}

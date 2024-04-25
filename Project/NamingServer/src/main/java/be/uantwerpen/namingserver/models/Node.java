package be.uantwerpen.namingserver.models;

import be.uantwerpen.namingserver.utils.HashingFunction;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.net.InetSocketAddress;
import java.time.LocalDateTime;

public class Node {

    private String name;
    private InetSocketAddress socketAddress;
    @JsonIgnore
    private LocalDateTime lastPing;

    public Node() {}

    public Node(String name, InetSocketAddress address) {
        this.name = name;
        this.socketAddress = address;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAddress(InetSocketAddress address) {
        this.socketAddress = address;
    }

    public void setLastPing(LocalDateTime lastPing) {
        this.lastPing = lastPing;
    }

    public String getName() {
        return name;
    }

    public InetSocketAddress getAddress() {
        return socketAddress;
    }

    public String getSocketAddress() {
        return socketAddress.getHostString() + ":" + socketAddress.getPort();
    }

    public LocalDateTime getLastPing() {
        return lastPing;
    }

    @Override
    public int hashCode() {
        return HashingFunction.getHashFromString(name);
    }
}

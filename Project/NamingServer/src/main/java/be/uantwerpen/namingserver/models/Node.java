package be.uantwerpen.namingserver.models;

import be.uantwerpen.namingserver.utils.HashingFunction;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.net.InetAddress;
import java.time.LocalDateTime;

public class Node {

    private String name;
    private InetAddress address;
    @JsonIgnore
    private LocalDateTime lastPing;

    public Node() {}

    public Node(String name, InetAddress address) {
        this.name = name;
        this.address = address;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAddress(InetAddress address) {
        this.address = address;
    }

    public void setLastPing(LocalDateTime lastPing) {
        this.lastPing = lastPing;
    }

    public String getName() {
        return name;
    }

    public InetAddress getAddress() {
        return address;
    }

    public LocalDateTime getLastPing() {
        return lastPing;
    }

    @Override
    public int hashCode() {
        return HashingFunction.getHashFromString(name);
    }
}

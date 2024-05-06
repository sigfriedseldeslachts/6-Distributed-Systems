package be.uantwerpen.namingserver.models;

import be.uantwerpen.namingserver.utils.HashingFunction;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.net.InetSocketAddress;
import java.time.LocalDateTime;

public class Node {

    private String name;
    private InetSocketAddress socketAddress;
    @JsonIgnore
    private LocalDateTime lastPing = LocalDateTime.now();
    private boolean leaving = false;

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

    public boolean isLeaving() {
        return leaving;
    }

    public void setLeaving() {
        this.leaving = true;
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

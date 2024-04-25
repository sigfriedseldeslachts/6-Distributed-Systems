package be.uantwerpen.distributedclients.models;

import be.uantwerpen.distributedclients.utils.HashingFunction;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class Node {

    private String name;
    private InetSocketAddress socketAddress;

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

    public String getName() {
        return name;
    }

    public InetSocketAddress getAddress() {
        return socketAddress;
    }

    public String getSocketAddress() {
        return socketAddress.getHostString() + ":" + socketAddress.getPort();
    }

    @Override
    public int hashCode() {
        return HashingFunction.getHashFromString(name);
    }
}

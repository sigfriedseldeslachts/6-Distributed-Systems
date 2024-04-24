package be.uantwerpen.distributedclients.models;

import be.uantwerpen.distributedclients.utils.HashingFunction;

import java.net.InetAddress;

public class Node {

    private String name;
    private InetAddress address;

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

    public String getName() {
        return name;
    }

    public InetAddress getAddress() {
        return address;
    }

    @Override
    public int hashCode() {
        return HashingFunction.getHashFromString(name);
    }
}
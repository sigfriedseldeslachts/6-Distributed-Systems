package be.uantwerpen.namingserver.models;

import be.uantwerpen.namingserver.utils.HashingFunction;

import java.net.InetAddress;

public class Node {

    private String name;
    private InetAddress address;

    public Node(String name) {
        this.name = name;
    }

    @Override
    public int hashCode() {
        return HashingFunction.getHashFromString(name);
    }
}

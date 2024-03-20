package be.uantwerpen.namingserver.models;

import java.util.Objects;

public class Node {

    private String name;

    public Node(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return Objects.equals(name, node.name);
    }

    @Override
    public int hashCode() {
        return (int) mapRange(Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 32768, name.hashCode();
    }

    public static double mapRange(double a1, double a2, double b1, double b2, double s){
        return b1 + ((s - a1) * (b2 - b1)) / (a2 - a1);
    }
}

package be.uantwerpen.namingserver.utils;

public class HashingFunction {

    public static int getHashFromString(String name) {
        return (int) mapRange(Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 32768, name.hashCode());
    }

    public static double mapRange(double a1, double a2, double b1, double b2, double s){
        return b1 + ((s - a1) * (b2 - b1)) / (a2 - a1);
    }

}

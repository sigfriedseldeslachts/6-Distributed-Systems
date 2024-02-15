package be.uantwerpen;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;

public class Main {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("ERROR: Please specify host and port");
            return;
        }

        String hostname = args[0];
        int port = Integer.parseInt(args[1]);

        if (port < 1) {
            System.out.println("ERROR: Invalid port number");
            return;
        }

        System.out.println("Attempting connection to " + args[0] + ":" + args[1]);

        try (Socket socket = new Socket(hostname, port)) {
            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            System.out.println(reader.readLine());
        } catch (UnknownHostException e) {
            System.out.println("ERROR: Server not found! " + e.getMessage());
        } catch (IOException e) {
            System.out.println("ERROR: Input/Outpurt error! " + e.getMessage());
        }
    }
}
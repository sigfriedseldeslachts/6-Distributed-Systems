package be.uantwerpen;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

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
            // Data to server
            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);
            Scanner scanner = new Scanner(System.in);

            // Data from server
            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            while (true) {
                System.out.print("Write something: ");
                String text = scanner.nextLine();
                writer.println(text);
                System.out.println(reader.readLine());
            }
        } catch (UnknownHostException e) {
            System.out.println("ERROR: Server not found! " + e.getMessage());
        } catch (IOException e) {
            System.out.println("ERROR: Input/Outpurt error! " + e.getMessage());
        }
    }
}
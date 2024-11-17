package it.startup.sendudes.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class TCP_NetworkUtils {
    private static ServerSocket serverSocket;
    private static Socket clientSocket;
    private static PrintWriter out;
    private static BufferedReader in;

    public static void startServerConnection(int port) {
        // create a server socket on port number 9090
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server is running and waiting for client connection...");

            // Accept incoming client connection
            Socket clientSocket = serverSocket.accept();
            System.out.println("Client connected!");

            // Setup input and output streams for communication with the client
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

            // Read message from client
            String message = in.readLine();
            System.out.println("Client says: " + message);

            // Send response to the client
            out.println("Message received by the server.");

            // Close the client socket
            clientSocket.close();
            // Close the server socket
            serverSocket.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public static void clientConnection(String IP, int port) {
        Socket socket = null;
        try {
            socket = new Socket(IP, port);


            // Setup output stream to send data to the server
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            // Setup input stream to receive data from the server
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Send message to the server
            out.println("Hello from client!");

            // Receive response from the server
            String response = in.readLine();
            System.out.println("Server says: " + response);

            // Close the socket
            socket.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

}

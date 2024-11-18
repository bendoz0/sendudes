package it.startup.sendudes.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class TCP_NetworkUtils {

    private static boolean hasAccepted;
    private static String acceptedData;
    private static Socket clientSocket;
    private static ServerSocket serverSocket = null;
    public static boolean hasData;
    public static BufferedReader in;
    public static PrintWriter out;
    private static Thread decisionThread;
    private static String connectedClient;
    private static OnClientConnected actionOnClientConnect;
    private static OnClientDisconnect actionOnClientDisconnect;

    public static void startServerConnection(int port) {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server is running and waiting for client connection...");

            clientSocket = serverSocket.accept();
            System.out.println("Client connected!");
            connectedClient = clientSocket.getInetAddress().toString();
            if (actionOnClientConnect != null) {
                actionOnClientConnect.onConnected();
            }
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            String message = in.readLine();
            if (message != null) {
                hasData = true;
                acceptedData = message;

//                if (hasAccepted) {
//                    out.println("User accepted file.");
//                } else {
//                    out.println("User rejected file.");
//                }
            }

        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    public static void setActionOnClientConnect(OnClientConnected x) {
        actionOnClientConnect = x;
    }
    public static void setActionOnClientDisconnect(OnClientDisconnect x) {
        actionOnClientDisconnect = x;
    }

    public static String getConnectedClient() {
        return connectedClient;
    }

    public static void userDecision(String msg) {
        decisionThread = new Thread(() -> {
            try {
                out.println(msg);
                if (msg.equals("accept")) hasAccepted=true;
                clientSocket.close();
                serverSocket.close();
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        });
        decisionThread.start();
        try {
            decisionThread.join();
            actionOnClientDisconnect.onDisconnected();
        } catch (InterruptedException e) {
            System.err.println(e.getMessage());
        }
    }



    public static void clientConnection(String IP, int port) {
        Socket socket = null;
        try {
            socket = new Socket(IP, port);


            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println("Hello from client!");

            String response = in.readLine();
            System.out.println("Server says: " + response);

            socket.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public static String getAcceptedData() {
        return acceptedData;
    }
}

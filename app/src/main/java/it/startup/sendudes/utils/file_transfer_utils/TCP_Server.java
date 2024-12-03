package it.startup.sendudes.utils.file_transfer_utils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import it.startup.sendudes.utils.file_transfer_utils.tcp_events.OnClientConnected;
import it.startup.sendudes.utils.file_transfer_utils.tcp_events.OnClientDisconnect;

public class TCP_Server {
    private static String acceptedData;
    private static Socket clientSocket;
    private static ServerSocket serverSocket = null;
    public static BufferedReader in;
    public static PrintWriter out;
    private static Thread decisionThread;
    private static String connectedClient;
    private static OnClientConnected actionOnClientConnect;
    private static OnClientDisconnect actionOnClientDisconnect;

    public static void startServerConnection(ServerSocket serverSocket) {
        while (!serverSocket.isClosed()) {
            try {
//                serverSocket = new ServerSocket(port);
                System.out.println("Server is running and waiting for client connection...");

                clientSocket = serverSocket.accept();
                System.out.println("Client connected!");
                connectedClient = clientSocket.getInetAddress().toString();
                if (actionOnClientConnect != null) actionOnClientConnect.onConnected();

                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);

                String receivedTransferProperties = in.readLine();
                if (receivedTransferProperties != null && !receivedTransferProperties.isEmpty()) {
                    FileTransferPacket deserializedObject = FileTransferPacket.fromJson(receivedTransferProperties);
                    Log.d("Server", "File transfer properties: " + deserializedObject.toString());
                    acceptedData = deserializedObject.toString();

                } else {
                    closeConnections();
                }
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
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
            out.println(msg);
            String s = "";
            StringBuilder complete_bytes = new StringBuilder();
            try{
                do{
                    s = in.readLine();
                    if (s != null) { // Check if the line is not null
                        Log.d("BYTES READ", s); // Log the read line
                        complete_bytes.append(s);
                        out.println(complete_bytes);
                    }
                }while(s != null);
            } catch (Exception e){
                Log.d("BYTE READING ERR", "Line 73 of TCP_SERVER.JAVA\n" + e.getMessage() );
            }
        });
        decisionThread.start();
        try {
            decisionThread.join();
            closeConnections();
        } catch (InterruptedException e) {
            System.err.println(e.getMessage());
        }
    }

    public static void closeConnections() {
        try {
            clientSocket.close();
//            serverSocket.close();
            actionOnClientDisconnect.onDisconnected();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getAcceptedData() {
        return acceptedData;
    }
}

package it.startup.sendudes.utils.file_transfer_utils;


import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import static it.startup.sendudes.utils.IConstants.MSG_ACCEPT_CLIENT;
import static it.startup.sendudes.utils.IConstants.MSG_BUSY_CLIENT;
import static it.startup.sendudes.utils.IConstants.MSG_CLIENT_RECEIVING;
import static it.startup.sendudes.utils.IConstants.MSG_FILETRANSFER_FINISHED;
import static it.startup.sendudes.utils.IConstants.MSG_REJECT_CLIENT;

import it.startup.sendudes.utils.file_transfer_utils.tcp_events.OnClientConnected;
import it.startup.sendudes.utils.file_transfer_utils.tcp_events.OnClientDisconnect;

public class TcpServer {
    private static String acceptedData;
    private static Socket clientSocket;
    private static ServerSocket serverSocket = null;
    public static BufferedReader in;
    public static PrintWriter out;
    private static Thread decisionThread;
    private static String connectedClient;
    private static OnClientConnected actionOnClientConnect;
    private static OnClientDisconnect actionOnClientDisconnect;
    private static FileTransferPacket fileDetails;
    private static boolean connectionOccupied = false;

    public static void startServerConnection(ServerSocket serverSocket) {//TODO: refactor
        while (!serverSocket.isClosed()) {
            try {
//                serverSocket = new ServerSocket(port);
//                System.out.println("Server is running and waiting for client connection...");

                Socket tmpclientSocket = serverSocket.accept();
                if (!connectionOccupied) {
                    clientSocket = tmpclientSocket;
                    System.out.println("Client connected!");
                    connectedClient = clientSocket.getInetAddress().toString();

                    in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    out = new PrintWriter(clientSocket.getOutputStream(), true);

                    String receivedTransferProperties = in.readLine();
                    if (receivedTransferProperties != null && !receivedTransferProperties.isEmpty()) {
                        FileTransferPacket deserializedObject = FileTransferPacket.fromJson(receivedTransferProperties);
                        Log.d("Server", "File transfer properties: " + deserializedObject.toString());
                        acceptedData = deserializedObject.toString();
                        fileDetails = deserializedObject;
                        connectionOccupied = true;
                        if (actionOnClientConnect != null) actionOnClientConnect.onConnected();
                    } else {
                        closeConnections();
                    }
                } else {
                    PrintWriter busyOut = new PrintWriter(tmpclientSocket.getOutputStream(), true);
                    busyOut.println(MSG_BUSY_CLIENT);
                    tmpclientSocket.close();
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

    static public void rejectFileFromSocket() {
        out.println(MSG_REJECT_CLIENT);
        closeConnections();
    }

    static public void acceptFileFromSocket() {//TODO: refactor and spilt in multiple function + better error handling (close outputStream in case of errors)
        try {
            out.println(MSG_ACCEPT_CLIENT);

            int fileSize = (int) fileDetails.getFileSize(); // Expected file size

            int nTotalRead = 0; // Tracks the total bytes read
            int nBytesRead;
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            String path = dir.getPath() + "/" + fileDetails.getFileName();
            File file = new File(path);

            if (!file.exists()) {
                file.createNewFile();
                FileOutputStream fos = new FileOutputStream(file, true);
                // Reading raw bytes from the input stream
                InputStream socketInputStream = clientSocket.getInputStream();
                byte[] bytesReceived = new byte[16 * 1024];
                while (nTotalRead < fileSize) {
                    nBytesRead = socketInputStream.read(bytesReceived);
                    if (nBytesRead == -1) {
                        break;
                    }
                    fos.write(bytesReceived, 0, nBytesRead);
                    fos.flush();
                    nTotalRead += nBytesRead;
                    double progress = ((double) nTotalRead / (double) fileSize) * 100;
                    Log.d("FILE TRANSFER", "Progress:" + String.format("%.0f", progress) + "%");
                }
                out.println(MSG_FILETRANSFER_FINISHED);
                fos.close();
            }
        } catch (Exception e) {
            Log.d("BYTE READING ERR", "Error receiving file: " + e.getMessage());
        }
        closeConnections();
    }

    public static void closeConnections() {
        try {
            clientSocket.close();
//            serverSocket.close();
            if (actionOnClientConnect != null) actionOnClientDisconnect.onDisconnected();
            connectionOccupied = false;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static FileTransferPacket getAcceptedObject() {
        return fileDetails;
    }
}

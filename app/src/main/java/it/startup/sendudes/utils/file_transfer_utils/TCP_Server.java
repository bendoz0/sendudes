package it.startup.sendudes.utils.file_transfer_utils;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

import static it.startup.sendudes.utils.IConstants.MSG_ACCEPT_CLIENT;
import static it.startup.sendudes.utils.IConstants.MSG_BUSY_CLIENT;

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
    private static FileTransferPacket fileDetails;

    private static boolean connectionOccupied = false;

    public static void startServerConnection(ServerSocket serverSocket) {
        while (!serverSocket.isClosed()) {
            try {
//                serverSocket = new ServerSocket(port);
//                System.out.println("Server is running and waiting for client connection...");

                Socket tmpclientSocket = serverSocket.accept();
                if(!connectionOccupied){
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
                }else{
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

    public static void userDecision(String msg) {
            out.println(msg);
            if(msg.equals(MSG_ACCEPT_CLIENT)){
                String s = "";
//                StringBuilder complete_bytes = new StringBuilder();
                int fileSize = (int) fileDetails.getFileSize();
                byte[] allBytes = new byte[fileSize];
                try{
                    do{
                        s = in.readLine();
                        int count = 0;
                        if (s != null) { // Check if the line is not null
//                            complete_bytes.append(s);
                            // TODO: needs fixing (sends corrupted files if not txt)
                            for(char c : s.toCharArray()){
                                byte b = (byte) c;
                                allBytes[count] = b;
                                count++;
                            }
                        }
                    }while(s != null);
                Log.d("OUTCOME", Arrays.toString(allBytes));
                writeToFile(allBytes, fileDetails.getFileName());
//                    Log.d("String builder", "" + complete_bytes.length());
                } catch (Exception e){
                    Log.d("BYTE READING ERR", "Line 73 of TCP_SERVER.JAVA\n" + e.getMessage() );
                }
            }
            closeConnections();
            connectionOccupied = false;
    }

    public static void writeToFile(byte[] array, String fileName)
    {
        try
        {
            String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + "/" + fileName;
            File file = new File(path);
            if (!file.exists()) {
                file.createNewFile();
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(array);
                String filePath = file.getPath();
                Log.d("PATHHHHHHHHHHHHHHHHHHHHH", filePath);
                fos.close();
            }
        } catch (FileNotFoundException e1) {
            Log.d("File not found", e1.getMessage());
        } catch (IOException e) {
            Log.d("IO error", e.getMessage());
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

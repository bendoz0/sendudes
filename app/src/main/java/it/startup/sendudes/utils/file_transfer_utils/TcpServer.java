package it.startup.sendudes.utils.file_transfer_utils;

import android.content.Context;

import android.media.MediaScannerConnection;

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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static it.startup.sendudes.utils.IConstants.MSG_ACCEPT_CLIENT;
import static it.startup.sendudes.utils.IConstants.MSG_BUSY_CLIENT;
import static it.startup.sendudes.utils.IConstants.MSG_FILETRANSFER_FINISHED;
import static it.startup.sendudes.utils.IConstants.MSG_REJECT_CLIENT;

import it.startup.sendudes.utils.Db.FilesDbAdapter;
import it.startup.sendudes.utils.file_transfer_utils.tcp_events.OnClientConnected;
import it.startup.sendudes.utils.file_transfer_utils.tcp_events.OnClientDisconnect;
import it.startup.sendudes.utils.file_transfer_utils.tcp_events.ProgressListener;
import it.startup.sendudes.utils.network_discovery.NetworkUtils;

public class TcpServer {
    private static String acceptedData;
    private static Socket clientSocket;
    private static ServerSocket serverSocket = null;

    public static BufferedReader in;
    public static PrintWriter out;
    private static String connectedClient;
    private static OnClientConnected actionOnClientConnect;
    private static OnClientDisconnect actionOnClientDisconnect;
    private static FileTransferPacket fileDetails;
    private static boolean connectionOccupied = false;
    private static FilesDbAdapter db;

    public static void startServerConnection(ServerSocket serverSocket) {//TODO: refactor
        if (serverSocket == null) return;
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
        if (out != null && clientSocket != null) {
            out.println(MSG_REJECT_CLIENT);
            closeConnections();
        }
    }


    static public void acceptFileFromSocket(Context context, ProgressListener listener) {
        out.println(MSG_ACCEPT_CLIENT);
        try {
            //filePath rappresenta il percorso di salvataggio del file
            String filePath = prepareFilePath();
            int fileSize = (int) fileDetails.getFileSize(); // Expected file size
            //chiamata del metodo per il trasferimento del file
            transferFile(clientSocket.getInputStream(), filePath, fileSize, listener);

            MediaScannerConnection.scanFile(context, new String[]{filePath}, null, (path, uri) -> {
                String uriContent = uri.toString();
                db = new FilesDbAdapter(context).open();
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
                LocalDateTime dateNow = LocalDateTime.now();
                long outcome = db.createFileRow(fileDetails.getFileName(), NetworkUtils.readableFileSize(fileDetails.getFileSize()), dtf.format(dateNow), 0, uriContent);
                if (outcome == -1) Log.d("INSERT INTO", "ERROOORRRRRRRRREEEEEE");
            });

            out.println(MSG_FILETRANSFER_FINISHED);
        } catch (Exception e) {
            Log.d("FILE TRANSFER ERROR", "Error during file transfer: " + e.getMessage());
        } finally {
            closeConnections();
        }
    }

    //metodo per gestire la creazione del file
    private static String prepareFilePath() throws IOException {
        //percorso della directory del download
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        //costruzione del percorso completo del file
        String path = dir.getPath() + "/" + fileDetails.getFileName();
        File file = new File(path);

        if (!file.exists()) {
            if (!file.createNewFile()) {
                throw new IOException("Failed to create file: " + path);
            }
        }
        return path;
    }

    //metodo per gestire il trasferimento del file
    private static void transferFile(InputStream socketInputStream, String filePath, int fileSize, ProgressListener listener) {
        int nTotalRead = 0;  // Tracks the total bytes read
        int nBytesRead;      //variabile per tracciare i byte letti ad ogni ciclo
        byte[] buffer = new byte[16 * 1024];

        //funzionalità try-with-resources per chiudere automaticamente le risorse FileOutputStream
        //chiude la risorsa quando si esce dal ciclo sia in caso positivo che negativo
        try (FileOutputStream fos = new FileOutputStream(filePath, true)) {
            //ciclo per leggere i byte dal socket fino al totale
            while (nTotalRead < fileSize) {
                nBytesRead = socketInputStream.read(buffer);
                if (nBytesRead == -1) {
                    throw new IOException("Unexpected end of stream");
                }
                //scrittura dei byte letti nel file
                fos.write(buffer, 0, nBytesRead);

                //assicuro che vengono scritti tutti
                fos.flush();

                //aggiorno i byte letti
                nTotalRead += nBytesRead;

                //calcolo la percentuale per terminare
                double progress = ((double) nTotalRead / (double) fileSize) * 100;
                listener.onProgressUpdate((int) progress);
                Log.d("FILE TRANSFER", "Progress: " + String.format("%.0f", progress) + "%");
            }
        } catch (IOException e) {
            Log.e("FILE TRANSFER ERROR", "Error during file transfer: " + e.getMessage());
            throw new RuntimeException("File transfer failed", e);
        }
    }


    public static void closeConnections() {
        try {
            clientSocket.close();
//            serverSocket.close();
            if (actionOnClientConnect != null) actionOnClientDisconnect.onDisconnected();
            connectionOccupied = false;
        } catch (Exception e) {
            Log.d("TCP: ", "Connection is closed");
        }
    }

    public static FileTransferPacket getAcceptedObject() {
        return fileDetails;
    }
}

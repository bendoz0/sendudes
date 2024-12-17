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
import static it.startup.sendudes.utils.IConstants.MSG_FILETRANSFER_ERROR;
import static it.startup.sendudes.utils.IConstants.MSG_FILETRANSFER_FINISHED;
import static it.startup.sendudes.utils.IConstants.MSG_REJECT_CLIENT;

import it.startup.sendudes.utils.Db.FilesDbAdapter;
import it.startup.sendudes.utils.file_transfer_utils.tcp_events.OnClientConnected;
import it.startup.sendudes.utils.file_transfer_utils.tcp_events.OnClientDisconnect;
import it.startup.sendudes.utils.file_transfer_utils.tcp_events.ProgressListener;
import it.startup.sendudes.utils.network_discovery.NetworkUtils;

public class TcpServer {
    //Sockets
    private Socket clientSocket;
    private PrintWriter out;
    private String connectedClient;
    private FileTransferPacket fileDetails;
    private boolean connectionOccupied = false;
    private FilesDbAdapter db;
    private final ServerSocket serverSocket;

    //Events
    private OnClientConnected actionOnClientConnect;
    private OnClientDisconnect actionOnClientDisconnect;
    private ProgressListener actionOnProgressAdvance;

    public TcpServer(int port) throws IOException {
        serverSocket = new ServerSocket(port, 1);
    }

    public void listenForTransferRequests() {
        while (!serverSocket.isClosed()) {
            try {
                Socket tmpclientSocket = serverSocket.accept();
                if (!connectionOccupied) {
                    clientSocket = tmpclientSocket;
                    System.out.println("Client connected!");
                    connectedClient = clientSocket.getInetAddress().toString();

                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    out = new PrintWriter(clientSocket.getOutputStream(), true);

                    String receivedTransferProperties = in.readLine();
                    if (receivedTransferProperties != null && !receivedTransferProperties.isEmpty()) {
                        fileDetails = FileTransferPacket.fromJson(receivedTransferProperties);
                        connectionOccupied = true;
                        if (actionOnClientConnect != null)
                            actionOnClientConnect.onConnected(fileDetails);
                    } else {
                        closeClientSocket();
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

    public void rejectFileFromSocket() {
        if (out != null && clientSocket != null) {
            out.println(MSG_REJECT_CLIENT);
            closeClientSocket();
        }
    }

    public void acceptFileFromSocket(Context context) {
        out.println(MSG_ACCEPT_CLIENT);
        try {
            String filePath = createNewFileInDownloadDir();
            int fileSize = (int) fileDetails.getFileSize(); // Expected file size
            receiveFileAndWriteToDisk(clientSocket.getInputStream(), filePath, fileSize);
            DbLogFileTransfer(context, filePath);
            out.println(MSG_FILETRANSFER_FINISHED);
        } catch (Exception e) {
            Log.d("FILE TRANSFER ERROR", "Error during file transfer: " + e.getMessage());
            out.println(MSG_FILETRANSFER_ERROR);
        } finally {
            closeClientSocket();
        }
    }

    private void DbLogFileTransfer(Context context, String filePath) {//TODO: refactor
        MediaScannerConnection.scanFile(context, new String[]{filePath}, null, (path, uri) -> {
            String uriContent = uri.toString();
            db = new FilesDbAdapter(context).open();
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            LocalDateTime dateNow = LocalDateTime.now();
            long outcome = db.createFileRow(fileDetails.getFileName(), NetworkUtils.readableFileSize(fileDetails.getFileSize()), dtf.format(dateNow), 0, uriContent);
            if (outcome == -1) Log.d("INSERT INTO", "ERROOORRRRRRRRREEEEEE");
        });
    }

    /**
     * @return the path of the file created
     */
    private String createNewFileInDownloadDir() throws IOException {
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        String path;
        int attempt = 0;
        while (true) {
            path = dir.getPath() + "/" + (attempt == 0 ? "" : "Copy (" + attempt + ") - ") + fileDetails.getFileName();
            File file = new File(path);
            if (file.exists()) {
                attempt += 1;
            } else {
                break;
            }
        }
        return path;
    }


    private void receiveFileAndWriteToDisk(InputStream socketInputStream, String filePath, int fileSize) {
        int nTotalRead = 0;  // Tracks the total bytes read
        int nBytesRead;      //variabile per tracciare i byte letti ad ogni ciclo
        byte[] buffer = new byte[16 * 1024];

        while (nTotalRead < fileSize) {
            try (FileOutputStream fos = new FileOutputStream(filePath, true)) {
                nBytesRead = socketInputStream.read(buffer);//Read new bytes from socket stream
                if (nBytesRead == -1) {
                    throw new IOException("Unexpected end of stream");
                }
                fos.write(buffer, 0, nBytesRead);//Write to dick
                fos.flush();
                nTotalRead += nBytesRead;

                if (actionOnProgressAdvance != null) {
                    double progress = ((double) nTotalRead / (double) fileSize) * 100;
                    actionOnProgressAdvance.onProgressUpdate((int) progress);
                }
            } catch (IOException e) {
                Log.e("FILE TRANSFER ERROR", "Error during file transfer: " + e.getMessage());
                throw new RuntimeException("File transfer failed", e);
            }
        }
    }

    private void closeClientSocket() {
        try {
            clientSocket.close();
            if (actionOnClientConnect != null) actionOnClientDisconnect.onDisconnected();
            connectionOccupied = false;
        } catch (Exception e) {
            Log.d("TCP: ", "Connection is closed");
        }
    }

    /**
     * This function should be called when the activity stops\n
     * Close the server socket
     */
    public void closeServerSocket() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            Log.d("Tcp Server", "Server socket closed");
        }
    }

    //Events setters
    public void setActionOnProgressAdvance(ProgressListener x) {
        actionOnProgressAdvance = x;
    }

    public void setActionOnClientConnect(OnClientConnected x) {
        actionOnClientConnect = x;
    }

    public void setActionOnClientDisconnect(OnClientDisconnect x) {
        actionOnClientDisconnect = x;
    }
}

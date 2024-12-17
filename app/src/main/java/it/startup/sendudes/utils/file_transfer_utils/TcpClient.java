package it.startup.sendudes.utils.file_transfer_utils;

import static it.startup.sendudes.utils.IConstants.MSG_ACCEPT_CLIENT;
import static it.startup.sendudes.utils.IConstants.MSG_BUSY_CLIENT;
import static it.startup.sendudes.utils.IConstants.MSG_FILETRANSFER_ERROR;
import static it.startup.sendudes.utils.IConstants.MSG_FILETRANSFER_FINISHED;
import static it.startup.sendudes.utils.files_utils.FileUtils.getFileInfoFromUri;
import static it.startup.sendudes.utils.files_utils.FileUtils.getFileInputStreamFromURI;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import it.startup.sendudes.utils.Db.FilesDbAdapter;
import it.startup.sendudes.utils.file_transfer_utils.tcp_events.OnConnectionBusy;
import it.startup.sendudes.utils.file_transfer_utils.tcp_events.OnTransferError;
import it.startup.sendudes.utils.file_transfer_utils.tcp_events.OnTransferSuccessfull;
import it.startup.sendudes.utils.file_transfer_utils.tcp_events.ProgressListener;
import it.startup.sendudes.utils.files_utils.FileUtils;
import it.startup.sendudes.utils.network_discovery.NetworkUtils;

public class TcpClient {
    private OnConnectionBusy connectionBusyEvent;
    private OnTransferSuccessfull transferSuccessfulEvent;
    private OnTransferError transferErrorEvent;
    private FilesDbAdapter db;
    private Socket socket;

    //gestisce la connessione con il server, l'inizio dell'invio del pacchetto e getisce la risposta del server
    public void sendFileToServer(String IP, int port, Uri uri, String username, String message, Context context, ProgressListener listener) {
        if (socket != null) {
            Log.d("Error sending file", "Cant start a new transfer. Cause: Already transferring a file");
            return;
        }
        FileUtils.FileInfo fileInfoFromUri = null;

        if (uri != null) fileInfoFromUri = getFileInfoFromUri(context, uri);

        //funzionalità try-with-resources per chiudere automaticamente le risorse: Socket, PrintWriter e BufferedReader
        //chiude la risorsa quando si esce dal ciclo sia in caso positivo che negativo
        try {
            socket = new Socket(IP, port);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            FileTransferPacket fileDetails = new FileTransferPacket(username,
                    fileInfoFromUri != null ? fileInfoFromUri.name : "",
                    fileInfoFromUri != null ? fileInfoFromUri.size : 0,
                    message);
            out.println(FileTransferPacket.toJson(fileDetails));

            String response = in.readLine();
            System.out.println("Server says: " + response);

            // Verifica della risposta del server
            if (response != null) {
                if (response.equals(MSG_ACCEPT_CLIENT)) {
                    transferFile(socket, in, context, uri, fileInfoFromUri, listener);
                } else if (response.equals(MSG_BUSY_CLIENT)) {
                    handleServerBusy();
                } else if (response.equals(MSG_FILETRANSFER_ERROR)) {
                    //TODO
                }
            }

        } catch (IOException e) {
            System.err.println(e.getMessage());
            if (transferErrorEvent != null) transferErrorEvent.OnTransferFailed();
        }
        closeSendSocket();
    }

    // Metodo per gestire il trasferimento del file
    private void transferFile(Socket socket, BufferedReader in, Context context, Uri uri, FileUtils.FileInfo fileInfoFromUri, ProgressListener listener) throws IOException {
        try (BufferedOutputStream socketOutputStream = new BufferedOutputStream(socket.getOutputStream());
             InputStream fileInputStream = getFileInputStreamFromURI(context, uri)) {

            if (fileInputStream == null) {
                throw new IOException("ERROR OPENING FILE");
            }

            int bytesToSend, totalBytesSent = 0;
            byte[] buffer = new byte[16 * 1024];

            // Trasferimento del file al server
            while ((bytesToSend = fileInputStream.read(buffer)) != -1) {
                socketOutputStream.write(buffer, 0, bytesToSend);
                socketOutputStream.flush();
                totalBytesSent += bytesToSend;
                double progress = ((double) totalBytesSent / (double) fileInfoFromUri.size) * 100;
                listener.onProgressUpdate((int) progress);
                Log.d("FILE TRANSFER", "Progress:" + String.format("%.0f", progress) + "%");
            }

            // Lettura del messaggio di fine trasferimento dal server
            String fileTransferEnd = in.readLine();
            if (fileTransferEnd != null && fileTransferEnd.equals(MSG_FILETRANSFER_FINISHED)) {
                if (transferSuccessfulEvent != null) {
                    db = new FilesDbAdapter(context).open();
                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
                    LocalDateTime dateNow = LocalDateTime.now();
                    long outcome = db.createFileRow(fileInfoFromUri.name, NetworkUtils.readableFileSize(fileInfoFromUri.size), dtf.format(dateNow), 1, uri.toString());
                    if (outcome == -1) Log.d("INSERT INTO", "ERROOORRRRRRRRREEEEEE");
                    db.close();
                    transferSuccessfulEvent.onTransferFinished();
                }
            }

        } catch (Exception e) {
            System.err.println("Error during file transfer: " + e.getMessage());
            throw e;
        }
    }

    // Metodo per gestire la risposta quando il server è occupato
    private void handleServerBusy() {
        Log.d("BUSY CLIENT", "Server is busy, try again later.");
        if (connectionBusyEvent != null) connectionBusyEvent.onConnectionBusy();
    }

    public void closeSendSocket() {
        try {
            if (socket != null) {
                socket.close();
                socket = null;
            }
        } catch (IOException e) {
            Log.d("Close socket error", "closeSendSocket: " + e.getMessage());
        }
    }

    //Event triggered when the file has been sent successfully
    public void setTransferSuccessfullEvent(OnTransferSuccessfull transferSuccessfullEvent) {
        this.transferSuccessfulEvent = transferSuccessfullEvent;
    }

    public void setTransferErrorEvent(OnTransferError transferErrorEvent) {
        this.transferErrorEvent = transferErrorEvent;
    }

    public void setConnectionBusyEvent(OnConnectionBusy connectionBusyEvent) {
        this.connectionBusyEvent = connectionBusyEvent;
    }
}

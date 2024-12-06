package it.startup.sendudes.utils.file_transfer_utils;

import static it.startup.sendudes.utils.IConstants.MSG_ACCEPT_CLIENT;
import static it.startup.sendudes.utils.IConstants.MSG_BUSY_CLIENT;
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

import it.startup.sendudes.utils.file_transfer_utils.tcp_events.OnConnectionBusy;
import it.startup.sendudes.utils.file_transfer_utils.tcp_events.OnTransferError;
import it.startup.sendudes.utils.file_transfer_utils.tcp_events.OnTransferSuccessfull;
import it.startup.sendudes.utils.files_utils.FileUtils;

public class TcpClient {
    private OnConnectionBusy connectionBusyEvent;
    private OnTransferSuccessfull transferSuccessfulEvent;
    private OnTransferError transferErrorEvent;

    public void sendFileToServer(String IP, int port, Uri uri, String username, String message, Context context) {//TODO: refactor and spilt in multiple function + better error handling (close outputStream in case of errors)
        FileUtils.FileInfo fileInfoFromUri = getFileInfoFromUri(context, uri);
        try {
            Socket socket = new Socket(IP, port);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            FileTransferPacket data = new FileTransferPacket(username, fileInfoFromUri.name, fileInfoFromUri.size, message);
            out.println(FileTransferPacket.toJson(data));

            String response = in.readLine();
            System.out.println("Server says: " + response);

            //Condizione che viene eseguita quando il server ritorna "ACCEPT
            if (response.equals(MSG_ACCEPT_CLIENT)) {
                BufferedOutputStream socketOutputStream = new BufferedOutputStream(socket.getOutputStream());

                InputStream fileInputStream = getFileInputStreamFromURI(context, uri);
                if (fileInputStream == null) {
                    throw new IOException("ERROR OPENING FILE");
                }
                int bytesToSend, totalBytesSent = 0;
                byte[] buffer = new byte[16 * 1024];
                while ((bytesToSend = fileInputStream.read(buffer)) != -1) {
                    socketOutputStream.write(buffer, 0, bytesToSend);
                    socketOutputStream.flush();
                    totalBytesSent += bytesToSend;
                    double progress = ((double)totalBytesSent/(double) (fileInfoFromUri.size)  * 100);
                    Log.d("FILE TRANSFER", "Progress:" + String.format("%.0f", progress) + "%");
                }

                fileInputStream.close();
                String fileTransferEnd = in.readLine();//Receive the message after transfering the file

                if (fileTransferEnd != null && fileTransferEnd.equals(MSG_FILETRANSFER_FINISHED)) {
                    if (transferSuccessfulEvent != null)
                        transferSuccessfulEvent.onTransferFinished();
                }

            } else if (response.equals(MSG_BUSY_CLIENT)) {
                Log.d("BUSYYYYYYYYYYYY", "BUSYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYY");
                if (connectionBusyEvent != null) connectionBusyEvent.onConnectionBusy();
            }
            socket.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
            if (transferErrorEvent != null) transferErrorEvent.OnTransferFailed();
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

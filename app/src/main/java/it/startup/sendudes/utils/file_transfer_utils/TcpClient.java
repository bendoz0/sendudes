package it.startup.sendudes.utils.file_transfer_utils;

import static it.startup.sendudes.utils.IConstants.MSG_ACCEPT_CLIENT;
import static it.startup.sendudes.utils.IConstants.MSG_BUSY_CLIENT;
import static it.startup.sendudes.utils.IConstants.MSG_FILETRANSFER_FINISHED;
import static it.startup.sendudes.utils.files_utils.FileUtils.getFileInfoFromUri;
import static it.startup.sendudes.utils.files_utils.FileUtils.readBytesFromUri;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import it.startup.sendudes.utils.file_transfer_utils.tcp_events.OnConnectionBusy;
import it.startup.sendudes.utils.file_transfer_utils.tcp_events.OnTransferError;
import it.startup.sendudes.utils.file_transfer_utils.tcp_events.OnTransferSuccessfull;
import it.startup.sendudes.utils.files_utils.FileUtils;

public class TcpClient {
    private OnConnectionBusy connectionBusyEvent;
    private OnTransferSuccessfull transferSuccessfullEvent;
    private OnTransferError transferErrorEvent;

    public void sendFileToServer(String IP, int port, Uri uri, Context context) {
        FileUtils.FileInfo fileInfoFromUri = getFileInfoFromUri(context, uri);
        try {
            Socket socket = new Socket(IP, port);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            FileTransferPacket data = new FileTransferPacket("TEST", fileInfoFromUri.name, fileInfoFromUri.size);
            out.println(FileTransferPacket.toJson(data));

            String response = in.readLine();
            System.out.println("Server says: " + response);

            //Condizione che viene eseguita quando il server ritorna "ACCEPT
            if (response.equals(MSG_ACCEPT_CLIENT)) {
                BufferedOutputStream outputStream = new BufferedOutputStream(socket.getOutputStream());
                byte[] buffer = readBytesFromUri(context, uri);
                if (buffer != null) {
                    outputStream.write(buffer, 0, buffer.length);
                }
                outputStream.flush();
                String error = in.readLine();
                if(error.equals(MSG_FILETRANSFER_FINISHED)){
                    if(transferSuccessfullEvent != null) transferSuccessfullEvent.onTransferFinished();
                }
            } else if (response.equals(MSG_BUSY_CLIENT)) {
                Log.d("BUSYYYYYYYYYYYY", "BUSYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYY");
                if (connectionBusyEvent != null) connectionBusyEvent.onConnectionBusy();
            }
            socket.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
            if(transferErrorEvent != null) transferErrorEvent.OnTransferFailed();
        }
    }

    //Event triggered when the file has been sent successfully
    public void setTransferSuccessfullEvent(OnTransferSuccessfull transferSuccessfullEvent) {
        this.transferSuccessfullEvent = transferSuccessfullEvent;
    }
    public void setTransferErrorEvent(OnTransferError transferErrorEvent) {
        this.transferErrorEvent = transferErrorEvent;
    }
    public void setConnectionBusyEvent(OnConnectionBusy connectionBusyEvent) {
        this.connectionBusyEvent = connectionBusyEvent;
    }
}

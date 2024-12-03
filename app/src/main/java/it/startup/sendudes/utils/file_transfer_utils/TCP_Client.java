package it.startup.sendudes.utils.file_transfer_utils;

import static it.startup.sendudes.utils.IConstants.MSG_ACCEPT_CLIENT;
import static it.startup.sendudes.utils.IConstants.MSG_BUSY_CLIENT;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import it.startup.sendudes.utils.file_transfer_utils.tcp_events.OnConnectionBusy;

public class TCP_Client {
    private static OnConnectionBusy connectionBusyEvent;

    public static void setConnectionBusyEvent(OnConnectionBusy connectionBusyEvent) {
        TCP_Client.connectionBusyEvent = connectionBusyEvent;
    }

    public static void clientConnection(String IP, int port, File fileToSend) {
        Socket socket = null;
        String fileName = fileToSend.getName();
        long fileSize = fileToSend.length();

        if (fileName != null && !fileName.isEmpty() && fileSize > 0) {
            try {
                socket = new Socket(IP, port);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                FileTransferPacket data = new FileTransferPacket("TEST", fileName, fileSize);
                out.println(FileTransferPacket.toJson(data));

                String response = in.readLine();
                System.out.println("Server says: " + response);

                //Condizione che viene eseguita quando il server ritorna "ACCEPT
                if (response.equals(MSG_ACCEPT_CLIENT)) {
                    BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(fileToSend));
                    BufferedOutputStream outputStream = new BufferedOutputStream(socket.getOutputStream());

                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    long totalBytesSent = 0;

                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                        totalBytesSent += bytesRead;
                    }
                    outputStream.flush();
                    inputStream.close();
                } else if (response.equals(MSG_BUSY_CLIENT)) {
                    Log.d("BUSYYYYYYYYYYYY", "BUSYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYY");
                    if (connectionBusyEvent != null) connectionBusyEvent.onConnectionBusy();
                }


                socket.close();
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }


    }




/*    private static void fileToSend(){
        try {
            Socket socket = new Socket(entry.getKey(), FILE_TRANSFER_PORT);
            BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file));
            BufferedOutputStream outputStream = new BufferedOutputStream(socket.getOutputStream());

            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytesSent = 0;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalBytesSent += bytesRead;
            }

            outputStream.flush();


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }*/

}

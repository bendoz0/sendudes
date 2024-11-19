package it.startup.sendudes.utils.file_transfer_utils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TCP_Client {
    public static void clientConnection(String IP, int port, String fileName, long fileSize) {
        Socket socket = null;
        try {
            socket = new Socket(IP, port);


            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            FileTransferPacket data = new FileTransferPacket("TEST", fileName, fileSize);
            out.println(data.toJson());

            String response = in.readLine();
            System.out.println("Server says: " + response);

            socket.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

}

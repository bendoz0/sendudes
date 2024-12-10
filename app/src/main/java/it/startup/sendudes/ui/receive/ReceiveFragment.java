package it.startup.sendudes.ui.receive;

import static it.startup.sendudes.utils.IConstants.*;
import static it.startup.sendudes.utils.file_transfer_utils.TcpServer.getAcceptedObject;

import static it.startup.sendudes.utils.file_transfer_utils.TcpServer.setActionOnClientConnect;
import static it.startup.sendudes.utils.file_transfer_utils.TcpServer.setActionOnClientDisconnect;
import static it.startup.sendudes.utils.file_transfer_utils.TcpServer.startServerConnection;
import static it.startup.sendudes.utils.files_utils.PermissionHandler.askForFilePermission;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;


import java.io.IOException;
import java.net.ServerSocket;
import java.util.Objects;

import it.startup.sendudes.databinding.FragmentReceiveBinding;
import it.startup.sendudes.utils.file_transfer_utils.FileTransferPacket;
import it.startup.sendudes.utils.file_transfer_utils.TcpServer;
import it.startup.sendudes.utils.network_discovery.UDP_NetworkUtils;

public class ReceiveFragment extends Fragment {
    private FragmentReceiveBinding binding;
    private UDP_NetworkUtils udpHandler;
    private Thread broadcastReplierThread;
    private Thread tcpSeverStarterThread;
    private ServerSocket fileTransferSocket;
    private FileTransferPacket fileInArrival;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentReceiveBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        binding.btnAcceptData.setEnabled(false);
        binding.btnRejectData.setEnabled(false);
        binding.twUserIp.setText(username);
        binding.cardView.setVisibility(View.GONE);

        try {
            udpHandler = new UDP_NetworkUtils(RECEIVE_PORT, PING_PORT, username);
            fileTransferSocket = new ServerSocket(FILE_TRANSFER_PORT, 1);
        } catch (IOException e) {
            Log.d("ERROR ON START", Objects.requireNonNull(e.getMessage()));
        }
        broadcastReplier();
        startFileTransferServer();
        askForFilePermission(this, () -> {
        });
    }


    @Override
    public void onResume() {
        super.onResume();
        setActionOnClientConnect(() -> {
            requireActivity().runOnUiThread(() -> {
                binding.btnAcceptData.setEnabled(true);
                binding.btnRejectData.setEnabled(true);
                showFilePropertiesInArrival();
            });
        });

        setActionOnClientDisconnect(() -> {
            requireActivity().runOnUiThread(() -> {
                binding.btnAcceptData.setEnabled(false);
                binding.btnRejectData.setEnabled(false);
                Toast.makeText(getContext(), "User disconnected", Toast.LENGTH_SHORT).show();
                clearFilePropertiesInArrival();
            });
        });

        binding.btnRejectData.setOnClickListener(v -> {
            new Thread(TcpServer::rejectFileFromSocket).start();
        });
        binding.btnAcceptData.setOnClickListener(v -> {
            new Thread(TcpServer::acceptFileFromSocket).start();
        });
    }

    private void showFilePropertiesInArrival() {
        fileInArrival = getAcceptedObject();
        binding.btnAcceptData.setEnabled(fileInArrival.getFileName() != null && !fileInArrival.getFileName().isEmpty());
        binding.btnRejectData.setText(binding.btnAcceptData.isEnabled() ? "Reject" : "Disconnect");

        binding.btnAcceptData.setVisibility(binding.btnAcceptData.isEnabled() ? View.VISIBLE : View.GONE);
        binding.cardView.setVisibility(View.VISIBLE);
        binding.fileSize.setVisibility(binding.btnAcceptData.isEnabled() ? View.VISIBLE : View.GONE);
        binding.fileName.setVisibility(binding.btnAcceptData.isEnabled() ? View.VISIBLE : View.GONE);

        binding.receivingFrom.setText("Receiving from: " + fileInArrival.getUserName());
        binding.fileName.setText((fileInArrival.getFileName().isEmpty() ? "" : "File name: " + fileInArrival.getFileName()));
        binding.fileSize.setText(fileInArrival.getFileSize() > 0 ? "File Size: " + fileInArrival.getFileSize() : "");
        binding.receivedMessage.setText(fileInArrival.getOptionalMessage().isEmpty() ? "" : "Message: " + fileInArrival.getOptionalMessage());
    }

    private void clearFilePropertiesInArrival() {
        binding.receivingFrom.setText("User disconnected");
        binding.fileName.setText("");
        binding.fileSize.setText("");
        binding.receivedMessage.setText("");
    }


    @Override
    public void onStop() {
        super.onStop();
        udpHandler.broadcast(MSG_CLIENT_NOT_RECEIVING);
        udpHandler.broadcast(MSG_CLIENT_NOT_RECEIVING);
        udpHandler.broadcast(MSG_CLIENT_NOT_RECEIVING);
        udpHandler.broadcast(MSG_CLIENT_NOT_RECEIVING);
        udpHandler.broadcast(MSG_CLIENT_NOT_RECEIVING);
        udpHandler.broadcast(MSG_CLIENT_NOT_RECEIVING);
        udpHandler.broadcast(MSG_CLIENT_NOT_RECEIVING);
        udpHandler.broadcast(MSG_CLIENT_NOT_RECEIVING);

        if (tcpSeverStarterThread != null && tcpSeverStarterThread.isAlive())
            tcpSeverStarterThread.interrupt();
        if (!fileTransferSocket.isClosed()) {
            try {
                fileTransferSocket.close();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
        udpHandler.closeSockets();
    }

    private void startFileTransferServer() {
        tcpSeverStarterThread = new Thread(() -> startServerConnection(fileTransferSocket));
        tcpSeverStarterThread.start();
    }

    private void broadcastReplier() {
        broadcastReplierThread = new Thread(() -> {
            try {
                udpHandler.startBroadcastHandshakeListener();//Funzione che blocca il codice in questo punto
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        });

        broadcastReplierThread.start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
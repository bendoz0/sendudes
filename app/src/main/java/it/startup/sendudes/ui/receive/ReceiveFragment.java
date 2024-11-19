package it.startup.sendudes.ui.receive;

import static it.startup.sendudes.utils.IConstants.FILE_TRANSFER_PORT;
import static it.startup.sendudes.utils.IConstants.MSG_CLIENT_NOT_RECEIVING;
import static it.startup.sendudes.utils.IConstants.MULTICAST_ADDRESS;
import static it.startup.sendudes.utils.IConstants.PING_PORT;
import static it.startup.sendudes.utils.IConstants.RECEIVE_PORT;
import static it.startup.sendudes.utils.file_transfer_utils.TCP_Server.getAcceptedData;
import static it.startup.sendudes.utils.file_transfer_utils.TCP_Server.getConnectedClient;
import static it.startup.sendudes.utils.file_transfer_utils.TCP_Server.setActionOnClientConnect;
import static it.startup.sendudes.utils.file_transfer_utils.TCP_Server.setActionOnClientDisconnect;
import static it.startup.sendudes.utils.file_transfer_utils.TCP_Server.startServerConnection;
import static it.startup.sendudes.utils.file_transfer_utils.TCP_Server.userDecision;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.io.IOException;
import java.net.ServerSocket;

import it.startup.sendudes.databinding.FragmentReceiveBinding;
import it.startup.sendudes.utils.UDP_NetworkUtils;

public class ReceiveFragment extends Fragment {
    private FragmentReceiveBinding binding;
    private UDP_NetworkUtils udpHandler;
    private Thread broadcastReplierThread;
    private Thread tcpSeverStarterThread;
    private ServerSocket fileTransferSocket;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentReceiveBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        binding.btnAcceptData.setEnabled(false);
        binding.btnRejectData.setEnabled(false);

        try {
            udpHandler = new UDP_NetworkUtils(PING_PORT,RECEIVE_PORT);
            fileTransferSocket = new ServerSocket(FILE_TRANSFER_PORT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        broadcastReplier();
        startServer();
    }


    @Override
    public void onResume() {
        super.onResume();
        setActionOnClientConnect(() -> {
            requireActivity().runOnUiThread(() -> {
                binding.btnAcceptData.setEnabled(true);
                binding.btnRejectData.setEnabled(true);
                binding.receivedData.setText(getAcceptedData());
            });
            binding.receiveTtile.setText(getConnectedClient());
        });

        setActionOnClientDisconnect(() -> {
            requireActivity().runOnUiThread(() -> {
                binding.btnAcceptData.setEnabled(false);
                binding.btnRejectData.setEnabled(false);
            });
            Toast.makeText(getContext(), "User disconnected", Toast.LENGTH_SHORT).show();
            binding.receiveTtile.setText("User Disconnected");
        });

        binding.btnRejectData.setOnClickListener(v -> {
            userDecision("reject");
        });
        binding.btnAcceptData.setOnClickListener(v -> {
            userDecision("accept");
            Log.d("FILE ACCEPTED", "FILE CONTAINS: "+ binding.receivedData.getText());
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        udpHandler.broadcast(MSG_CLIENT_NOT_RECEIVING);

        if (tcpSeverStarterThread != null && tcpSeverStarterThread.isAlive()) tcpSeverStarterThread.interrupt();
        udpHandler.closeSockets();
        if (!fileTransferSocket.isClosed()) {
            try {
                fileTransferSocket.close();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private void startServer() {
        tcpSeverStarterThread = new Thread(() -> {
            startServerConnection(fileTransferSocket);

        });
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
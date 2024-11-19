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
import static it.startup.sendudes.utils.UDP_NetworkUtils.broadcast;
import static it.startup.sendudes.utils.UDP_NetworkUtils.broadcastHandshake;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;


import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import it.startup.sendudes.databinding.FragmentReceiveBinding;

public class ReceiveFragment extends Fragment {

    private FragmentReceiveBinding binding;
    private DatagramSocket socket;
    private MulticastSocket listenerSocket;
    private Thread broadcastReplierThread;
    private Thread tcpSeverStarterThread;

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
            socket = new DatagramSocket(RECEIVE_PORT);
            listenerSocket = new MulticastSocket(PING_PORT);
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            listenerSocket.joinGroup(group);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        broadcastReplier();
        startServer();

        Log.d("MESSAGE: ", "STARTED SUCCESSFULLY: " + socket.isClosed());

    }


    @Override
    public void onResume() {
        super.onResume();
        setActionOnClientConnect(()->{
                requireActivity().runOnUiThread(() -> {
                    binding.btnAcceptData.setEnabled(true);
                    binding.btnRejectData.setEnabled(true);
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
            binding.receivedData.setText(getAcceptedData());
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        broadcast(socket, MSG_CLIENT_NOT_RECEIVING);
        if (broadcastReplierThread != null && broadcastReplierThread.isAlive())
            broadcastReplierThread.interrupt();
        if (tcpSeverStarterThread != null && tcpSeverStarterThread.isAlive())
            tcpSeverStarterThread.interrupt();
        if (!socket.isClosed()) socket.close();
        if (!listenerSocket.isClosed()) listenerSocket.close();
        Log.d("MESSAGE: ", "CLOSED SUCCESSFULLY: " + socket.isClosed());
    }

    private void startServer() {
        tcpSeverStarterThread = new Thread(() -> {
            startServerConnection(FILE_TRANSFER_PORT);

        });
        tcpSeverStarterThread.start();
    }

    private void broadcastReplier() {
        broadcastReplierThread = new Thread(() -> {
            try {
                broadcastHandshake(socket, listenerSocket);

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
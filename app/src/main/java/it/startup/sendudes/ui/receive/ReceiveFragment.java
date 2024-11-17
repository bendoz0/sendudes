package it.startup.sendudes.ui.receive;

import static it.startup.sendudes.utils.IConstants.FILE_TRANSFER_PORT;
import static it.startup.sendudes.utils.IConstants.MSG_CLIENT_NOT_RECEIVING;
import static it.startup.sendudes.utils.IConstants.PING_PORT;
import static it.startup.sendudes.utils.IConstants.RECEIVE_PORT;
import static it.startup.sendudes.utils.TCP_NetworkUtils.startServerConnection;
import static it.startup.sendudes.utils.UDP_NetworkUtils.broadcast;
import static it.startup.sendudes.utils.UDP_NetworkUtils.broadcastHandshake;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;


import java.net.DatagramSocket;
import java.net.SocketException;

import it.startup.sendudes.databinding.FragmentReceiveBinding;

public class ReceiveFragment extends Fragment {

    private FragmentReceiveBinding binding;
    private DatagramSocket socket;
    private DatagramSocket listenerSocket;
    private Thread broadcastReplierThread;


    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentReceiveBinding.inflate(inflater, container, false);

        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        try {
            socket = new DatagramSocket(RECEIVE_PORT);
            listenerSocket = new DatagramSocket(PING_PORT);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        broadcastReplier();
        startServerConnection(FILE_TRANSFER_PORT);
        Log.d("MESSAGE: ", "STARTED SUCCESSFULLY: " + socket.isClosed());

    }

    @Override
    public void onStop() {
        super.onStop();
        broadcast(socket,  MSG_CLIENT_NOT_RECEIVING);
        if (broadcastReplierThread != null && broadcastReplierThread.isAlive()) broadcastReplierThread.interrupt();
        if (!socket.isClosed()) socket.close();
        if (!listenerSocket.isClosed()) listenerSocket.close();
        Log.d("MESSAGE: ", "CLOSED SUCCESSFULLY: " + socket.isClosed());

    }

    private void broadcastReplier() {
        broadcastReplierThread = new Thread(() -> {
            try {
                broadcastHandshake(socket,listenerSocket);

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
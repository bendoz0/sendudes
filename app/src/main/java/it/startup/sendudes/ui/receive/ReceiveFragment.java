package it.startup.sendudes.ui.receive;

import static it.startup.sendudes.utils.NetworkUtils.broadcast;

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
    private final DatagramSocket socket;
    private Thread broadcastThread;

    public ReceiveFragment() throws SocketException {
        this.socket = new DatagramSocket();
    }


    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentReceiveBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        binding.btnSearchDevices.setOnClickListener(v -> onClickSearchDevices());

        broadcastRepeater();
        return root;
    }

    private void broadcastRepeater() {
        broadcastThread = new Thread(() -> {
            try {
                Thread.sleep(5000);
                while (!Thread.currentThread().isInterrupted()) {
                    broadcast(socket);
                    // todo: make it efficient
                    Thread.sleep(5000);
                }
            } catch (Exception e) {
                Log.d("Broadcast Repeater Thread", e.getMessage() == null ? "socket is null" : e.getMessage());
            }
        });

        broadcastThread.start();
    }

    private void onClickSearchDevices() {
        broadcast(socket);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (broadcastThread != null && broadcastThread.isAlive()) broadcastThread.interrupt();
        if (!socket.isClosed()) socket.close();
        binding = null;
    }
}
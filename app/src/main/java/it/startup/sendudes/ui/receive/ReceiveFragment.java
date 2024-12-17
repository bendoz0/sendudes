package it.startup.sendudes.ui.receive;

import static it.startup.sendudes.utils.IConstants.*;
import static it.startup.sendudes.utils.files_utils.PermissionHandler.askForFilePermission;
import static it.startup.sendudes.utils.network_discovery.NetworkUtils.readableFileSize;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;


import java.io.IOException;
import java.util.Objects;

import it.startup.sendudes.R;
import it.startup.sendudes.databinding.FragmentReceiveBinding;
import it.startup.sendudes.utils.file_transfer_utils.FileTransferPacket;
import it.startup.sendudes.utils.file_transfer_utils.TcpServer;
import it.startup.sendudes.utils.network_discovery.UDP_NetworkUtils;

public class ReceiveFragment extends Fragment {
    private FragmentReceiveBinding binding;
    private UDP_NetworkUtils udpHandler;
    private Thread broadcastReplierThread;
    private Thread tcpSeverStarterThread;
    private TcpServer fileTransferHandler;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentReceiveBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        createPulseAnimation();
        binding.btnAcceptData.setEnabled(false);
        binding.progressBar.setProgress(0);
        binding.btnRejectData.setEnabled(false);
        binding.twUserIp.setText(username);
        binding.cardView.setVisibility(View.GONE);

        try {
            udpHandler = new UDP_NetworkUtils(RECEIVE_PORT, PING_PORT, username);
            fileTransferHandler = new TcpServer(FILE_TRANSFER_PORT);
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
        clientConnector();
        clientDisconnector();
        binding.btnRejectData.setOnClickListener(v -> {
            rejectIncomingFile();
        });
        binding.btnAcceptData.setOnClickListener(v -> {
            new Thread(() -> {
                binding.progressBar.setProgress(0);
                fileTransferHandler.acceptFileFromSocket(getContext());
            }).start();
        });
        fileTransferHandler.setActionOnProgressAdvance(progress -> binding.progressBar.setProgress(progress, true));
    }

    @Override
    public void onStop() {
        super.onStop();
        //TODO:close server socket
        if (udpHandler != null) {
            for (int i = 0; i < 10; i++) {
                udpHandler.broadcast(MSG_CLIENT_NOT_RECEIVING);
            }
        }
        if (tcpSeverStarterThread != null && tcpSeverStarterThread.isAlive())
            tcpSeverStarterThread.interrupt();

        if (fileTransferHandler != null) {
            rejectIncomingFile();
            fileTransferHandler.closeServerSocket();
        }

        if (udpHandler != null) udpHandler.closeSockets();
        if (broadcastReplierThread != null) broadcastReplierThread.interrupt();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void createPulseAnimation() {
        Animation pulseAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.circular_pulse_animation);
        binding.pulseView.startAnimation(pulseAnimation);
    }

    private void clientDisconnector() {
        fileTransferHandler.setActionOnClientDisconnect(() -> {
            requireActivity().runOnUiThread(() -> {
                binding.btnAcceptData.setEnabled(false);
                binding.btnRejectData.setEnabled(false);
                binding.receivedDataContainer.setVisibility(View.GONE);
                binding.pulseView.setVisibility(View.VISIBLE);
                Toast.makeText(getContext(), "User disconnected", Toast.LENGTH_SHORT).show();
                clearFilePropertiesInArrival();
            });
        });
    }

    private void clientConnector() {
        fileTransferHandler.setActionOnClientConnect((fileTransferPacket) -> {
            requireActivity().runOnUiThread(() -> {
                binding.btnAcceptData.setEnabled(true);
                binding.btnRejectData.setEnabled(true);
                binding.pulseView.setVisibility(View.GONE);
                binding.receivedDataContainer.setVisibility(View.VISIBLE);
                showFilePropertiesInArrival(fileTransferPacket);
            });
        });
    }

    private void rejectIncomingFile() {
        new Thread(()->fileTransferHandler.rejectFileFromSocket()).start();
    }

    private void showFilePropertiesInArrival(FileTransferPacket fileDetails) {
        binding.btnAcceptData.setEnabled(fileDetails.getFileName() != null && !fileDetails.getFileName().isEmpty());
        binding.btnRejectData.setText(binding.btnAcceptData.isEnabled() ? "Reject" : "Disconnect");

        binding.btnAcceptData.setVisibility(binding.btnAcceptData.isEnabled() ? View.VISIBLE : View.GONE);
        binding.cardView.setVisibility(View.VISIBLE);
        binding.fileSize.setVisibility(binding.btnAcceptData.isEnabled() ? View.VISIBLE : View.GONE);
        binding.fileName.setVisibility(binding.btnAcceptData.isEnabled() ? View.VISIBLE : View.GONE);

        binding.receivingFrom.setText(String.format("%s%s", getString(R.string.receiving_from), fileDetails.getUserName()));
        binding.fileName.setText((fileDetails.getFileName().isEmpty() ? "" : "File name: " + fileDetails.getFileName()));
        binding.fileSize.setText(fileDetails.getFileSize() > 0 ? "File Size: " + readableFileSize(fileDetails.getFileSize()) : "");
        binding.receivedMessage.setText(fileDetails.getOptionalMessage().isEmpty() ? "" : "Message: " + fileDetails.getOptionalMessage());
    }

    private void clearFilePropertiesInArrival() {
        binding.receivingFrom.setText(R.string.user_disconnected);
        binding.fileName.setText("");
        binding.fileSize.setText("");
        binding.receivedMessage.setText("");
    }

    private void startFileTransferServer() {
        tcpSeverStarterThread = new Thread(() -> fileTransferHandler.listenForTransferRequests());
        tcpSeverStarterThread.start();
    }

    /**
     * Function that simulates a sort of handshake with another host
     */
    private void broadcastReplier() {
        broadcastReplierThread = new Thread(() -> {
            try {
                udpHandler.startBroadcastHandshakeListener();
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        });

        broadcastReplierThread.start();
    }
}
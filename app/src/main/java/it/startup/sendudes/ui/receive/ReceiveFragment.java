package it.startup.sendudes.ui.receive;

import static it.startup.sendudes.utils.IConstants.*;
import static it.startup.sendudes.utils.file_transfer_utils.TcpServer.getAcceptedObject;

import static it.startup.sendudes.utils.file_transfer_utils.TcpServer.setActionOnClientConnect;
import static it.startup.sendudes.utils.file_transfer_utils.TcpServer.setActionOnClientDisconnect;
import static it.startup.sendudes.utils.file_transfer_utils.TcpServer.startServerConnection;
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


import java.net.ServerSocket;
import java.util.Objects;

import it.startup.sendudes.R;
import it.startup.sendudes.databinding.FragmentReceiveBinding;
import it.startup.sendudes.utils.file_transfer_utils.FileTransferPacket;
import it.startup.sendudes.utils.file_transfer_utils.TcpServer;
import it.startup.sendudes.utils.network_discovery.NetworkConnectivityManager;
import it.startup.sendudes.utils.network_discovery.OnIPChangedListener;
import it.startup.sendudes.utils.network_discovery.UDP_NetworkUtils;

public class ReceiveFragment extends Fragment implements OnIPChangedListener {
    private FragmentReceiveBinding binding;
    private UDP_NetworkUtils udpHandler;
    private Thread broadcastReplierThread;
    private NetworkConnectivityManager networkConnectivityManager;
    private Thread tcpSeverStarterThread;
    private ServerSocket fileTransferSocket;
    private FileTransferPacket fileInArrival;


    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentReceiveBinding.inflate(inflater, container, false);
        networkConnectivityManager = new NetworkConnectivityManager(requireContext(), this);

        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        createPulseAnimation();
        binding.btnAcceptData.setEnabled(false);
        binding.progressBar.setProgress(0);
        binding.btnRejectData.setEnabled(false);
        binding.cardView.setVisibility(View.GONE);

        networkConnectivityManager.startListening();

        String currentIp = NetworkConnectivityManager.getCurrentIPAddress();
        if (!currentIp.equals("Cant find IP")) {
            try {
                binding.twUserIp.setText(username());
                udpHandler = new UDP_NetworkUtils(RECEIVE_PORT, PING_PORT);
                fileTransferSocket = new ServerSocket(FILE_TRANSFER_PORT, 1);

                broadcastReplier();
                startFileTransferServer();
            } catch (Exception e) {
                Log.d("ERROR ON START", Objects.requireNonNull(e.getMessage()));
            }
        }
        askForFilePermission(this, () -> {
        });
    }


    private void createPulseAnimation() {
        Animation pulseAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.circular_pulse_animation);
        binding.pulseView.startAnimation(pulseAnimation);
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
                TcpServer.acceptFileFromSocket(getContext(), progress -> {
                    binding.progressBar.setProgress(progress, true);
                });
            }).start();
        });
    }

    private void clientDisconnector() {
        setActionOnClientDisconnect(() -> {
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
        setActionOnClientConnect(() -> {
            requireActivity().runOnUiThread(() -> {
                binding.btnAcceptData.setEnabled(true);
                binding.btnRejectData.setEnabled(true);
                binding.pulseView.setVisibility(View.GONE);
                binding.receivedDataContainer.setVisibility(View.VISIBLE);
                showFilePropertiesInArrival();
            });
        });
    }

    private static void rejectIncomingFile() {
        new Thread(TcpServer::rejectFileFromSocket).start();
    }

    private void showFilePropertiesInArrival() {
        fileInArrival = getAcceptedObject();
        binding.btnAcceptData.setEnabled(fileInArrival.getFileName() != null && !fileInArrival.getFileName().isEmpty());
        binding.btnRejectData.setText(binding.btnAcceptData.isEnabled() ? "Reject" : "Disconnect");

        binding.btnAcceptData.setVisibility(binding.btnAcceptData.isEnabled() ? View.VISIBLE : View.GONE);
        binding.cardView.setVisibility(View.VISIBLE);
        binding.fileSize.setVisibility(binding.btnAcceptData.isEnabled() ? View.VISIBLE : View.GONE);
        binding.fileName.setVisibility(binding.btnAcceptData.isEnabled() ? View.VISIBLE : View.GONE);

        binding.receivingFrom.setText(String.format("%s%s", getString(R.string.receiving_from), fileInArrival.getUserName()));
        binding.fileName.setText((fileInArrival.getFileName().isEmpty() ? "" : "File name: " + fileInArrival.getFileName()));
        binding.fileSize.setText(fileInArrival.getFileSize() > 0 ? "File Size: " + readableFileSize(fileInArrival.getFileSize()) : "");
        binding.receivedMessage.setText(fileInArrival.getOptionalMessage().isEmpty() ? "" : "Message: " + fileInArrival.getOptionalMessage());
    }

    private void clearFilePropertiesInArrival() {
        binding.receivingFrom.setText(R.string.user_disconnected);
        binding.fileName.setText("");
        binding.fileSize.setText("");
        binding.receivedMessage.setText("");
    }


    @Override
    public void onStop() {
        super.onStop();

        if (udpHandler != null) {
            broadcastClientNotReceiving();
        }
        if (tcpSeverStarterThread != null && tcpSeverStarterThread.isAlive())
            tcpSeverStarterThread.interrupt();
        try {
            if (!fileTransferSocket.isClosed()) {
                rejectIncomingFile();
                fileTransferSocket.close();
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        if (udpHandler != null) udpHandler.closeSockets();
        networkConnectivityManager.stopListening();
    }

    private void broadcastClientNotReceiving() {
        for (int i = 0; i < 10; i++) {
            udpHandler.broadcast(MSG_CLIENT_NOT_RECEIVING);
        }
    }

    private void startFileTransferServer() {
        tcpSeverStarterThread = new Thread(() -> startServerConnection(fileTransferSocket));
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onNetworkChanged(String newIp) {
        requireActivity().runOnUiThread(() -> {
            binding.twUserIp.setText(username());
            try {
                if (udpHandler == null) {
                    udpHandler = new UDP_NetworkUtils(RECEIVE_PORT, PING_PORT);
                }
                if (fileTransferSocket == null)
                    fileTransferSocket = new ServerSocket(FILE_TRANSFER_PORT, 1);
                broadcastReplier();
                startFileTransferServer();

            } catch (Exception e) {
                Log.e("Network Change", "Failed to reinitialize UDP handler", e);
            }
        });
    }

    @Override
    public void onNoNetwork() {
        requireActivity().runOnUiThread(() -> {
            binding.twUserIp.setText(username());

            if (udpHandler != null) {
                udpHandler.closeSockets();
                udpHandler = null;
            }

            binding.btnAcceptData.setEnabled(false);
            binding.btnRejectData.setEnabled(false);
            binding.cardView.setVisibility(View.GONE);
        });
    }
}
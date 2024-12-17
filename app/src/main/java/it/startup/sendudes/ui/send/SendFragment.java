package it.startup.sendudes.ui.send;

import static it.startup.sendudes.utils.IConstants.FILE_TRANSFER_PORT;
import static it.startup.sendudes.utils.IConstants.MSG_CLIENT_PING;
import static it.startup.sendudes.utils.IConstants.PING_PORT;
import static it.startup.sendudes.utils.IConstants.RECEIVE_PORT;
import static it.startup.sendudes.utils.IConstants.username;
import static it.startup.sendudes.utils.files_utils.PermissionHandler.askForFilePermission;


import android.content.Intent;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;


import java.util.ArrayList;
import java.util.HashMap;

import it.startup.sendudes.R;
import it.startup.sendudes.databinding.FragmentSendBinding;
import it.startup.sendudes.utils.file_transfer_utils.IPAddressValidator;
import it.startup.sendudes.utils.file_transfer_utils.TcpClient;
import it.startup.sendudes.utils.file_transfer_utils.tcp_events.ProgressListener;
import it.startup.sendudes.utils.files_utils.FileThumbnailLoader;
import it.startup.sendudes.utils.files_utils.FileUtils;
import it.startup.sendudes.utils.network_discovery.OnListUpdate;
import it.startup.sendudes.utils.network_discovery.UDP_NetworkUtils;

public class SendFragment extends Fragment {
    //FragmentHomeBinding is a class generated automatically when View Binding is enabled (in the android v8 and later on ig)
    private FragmentSendBinding binding;
    private Thread broadcastHandshakeThread;
    private Thread tcpClientThread;
    private UDP_NetworkUtils udpHandler;
    private Uri selectedFileUri;
    private TcpClient tcpClient;
    private Handler handler;
    private ArrayAdapter<String> ipListAdapter;
    private boolean isScanning = false;
    private final int SCAN_DURATION_SECONDS = 2;


    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSendBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        try {
            udpHandler = new UDP_NetworkUtils(PING_PORT, RECEIVE_PORT, username);
            tcpClient = new TcpClient();
            ipListAdapter = new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    new ArrayList<>()
            );
        } catch (Exception e) {
            Log.d("SOCKET ERROR", e.getMessage() == null ? "its null" : e.getMessage());
        }
        uiActivityStart();
        broadcastPingEverySecond();
        broadcastHandshaker();
    }

    @Override
    public void onStop() {
        super.onStop();
        selectedFileUri = null;
        isScanning = false;
        //if (tcpClient != null) tcpClient.closeSendSocket();
        if (handler != null) handler.removeCallbacksAndMessages(null);
        if (broadcastHandshakeThread != null && broadcastHandshakeThread.isAlive())
            broadcastHandshakeThread.interrupt();
        if (tcpClientThread != null && tcpClientThread.isAlive()) tcpClientThread.interrupt();
        if (udpHandler != null) udpHandler.closeSockets();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void broadcastPingEverySecond() {
        handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (udpHandler != null) {
                    udpHandler.broadcast(MSG_CLIENT_PING);
                    handler.postDelayed(this, 3000);
                }


            }
        };
        handler.post(runnable); // Start the initial execution
    }

    private void uiActivityStart() {
        binding.twUserIp.setText(username);
        binding.progressBar.setProgress(0);
        binding.fileChosen.setOnClickListener(v -> onClickChooseFile());

        try {
            handleNetworkScanClick();

            handleCustomIpSetClick();

            handleFileSendOnUserClick();

            OnListUpdate updatedList = refreshScannedIpList();

            if (udpHandler != null) udpHandler.onListUpdate(updatedList);

            tcpStarter();
        } catch (Exception e) {
            Toast.makeText(getContext(), "no connection", Toast.LENGTH_SHORT).show();
        }
    }

    @NonNull
    private OnListUpdate refreshScannedIpList() {
        return scannedIPs -> {
            requireActivity().runOnUiThread(() -> {
                if (scannedIPs == null) {
                    Toast.makeText(getContext(), "no connection", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (scannedIPs.isEmpty()) {
                    try {
                        binding.scannedMsg.setVisibility(View.VISIBLE);
                        binding.foundIps.setVisibility(View.GONE);
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                } else {
                    binding.scannedMsg.setVisibility(View.GONE);
                    binding.foundIps.setVisibility(View.VISIBLE);
                    ipListAdapter.clear();
                    ipListAdapter.addAll(scannedIPs.values());
                    ipListAdapter.notifyDataSetChanged();
                }
            });
        };
    }

    private void handleFileSendOnUserClick() {
        binding.foundIps.setOnItemClickListener((adapterView, view, position, id) -> {
            String ip = (String) adapterView.getItemAtPosition(position);
            tryToSendFile(ip.split("#", 2)[1]);
        });
        binding.foundIps.setAdapter(ipListAdapter);
    }

    private void handleCustomIpSetClick() {
        binding.customIpSetBtn.setOnClickListener(v -> {
            if (binding.customIpInput.getText() != null && !binding.customIpInput.getText().toString().isEmpty()) {
                validateAndSendFile(binding.customIpInput.getText().toString());
            }
        });
    }

    private void validateAndSendFile(String ip) {
        if (IPAddressValidator.isValidIPv4(ip)) {
            tryToSendFile(ip);
        } else {
            Toast.makeText(getContext(), "Invalid User IP", Toast.LENGTH_LONG).show();
            binding.customIpInput.setText("");
        }
    }

    private void handleNetworkScanClick() {
        binding.btnNetworkScanner.setOnClickListener(v -> {
            if (udpHandler != null)
                onClickScanNetwork();
            else Toast.makeText(getContext(), "No internet found", Toast.LENGTH_LONG).show();

        });
    }

    void tryToSendFile(String ip) {
        if (canFileBeSent()) {
            startFileTransfer(ip);
        } else {
            Toast.makeText(getContext(), "Select a file or text", Toast.LENGTH_SHORT).show();
        }
    }

    private void onClickScanNetwork() {
        if (isScanning) return;
        isScanning = true;
        binding.btnNetworkScanner.setEnabled(false);

        requireActivity().runOnUiThread(() -> {
            binding.scanProgressBar.setVisibility(View.VISIBLE);
            binding.scannedMsg.setText(R.string.sendFragment_loading);
        });

        new Thread(this::initiateNetworkScan).start();
    }

    private void initiateNetworkScan() {
        try {
            long startTime = System.currentTimeMillis();
            long endTime = startTime + (SCAN_DURATION_SECONDS * 1000);

            while (System.currentTimeMillis() < endTime && isScanning) {
                if (udpHandler != null) {
                    udpHandler.scanNetwork();
                }
            }
        } catch (Exception e) {
            Log.e("ScanNetwork", "Error during network scan", e);
        } finally {
            isScanning = false;
            requireActivity().runOnUiThread(() -> {
                binding.btnNetworkScanner.setEnabled(true);
                binding.scanProgressBar.setVisibility(View.GONE);
                binding.scannedMsg.setText(R.string.no_user_found);
            });
        }
    }

    private void tcpStarter() {
        tcpClient.setTransferSuccessfullEvent(() ->
                requireActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "TRANSFER FINISHED SUCCESSFULLY", Toast.LENGTH_SHORT).show();
                            selectedFileUri = null;
                            binding.selectedFileThumbnail.setImageResource(0);
                            binding.fileChosen.setText(R.string.select_a_file);
                        }
                ));
        tcpClient.setConnectionBusyEvent(() ->
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "User device is transferring files", Toast.LENGTH_SHORT).show()
                )
        );
        tcpClient.setTransferErrorEvent(() ->
                requireActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "User not found", Toast.LENGTH_SHORT).show();
                        }
                ));
    }

    public void startFileTransfer(String ip) {
        try {
            binding.progressBar.setProgress(0);
            tcpClientThread = new Thread(() -> tcpClient.sendFileToServer(ip, FILE_TRANSFER_PORT, selectedFileUri, username, binding.optionalMessage.getText().toString(), getContext(), new ProgressListener() {
                @Override
                public void onProgressUpdate(int progress) {
                    if (binding != null && binding.progressBar != null)
                        binding.progressBar.setProgress(progress);
                }
            }));
            tcpClientThread.start();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Ops! Try again", Toast.LENGTH_SHORT).show();
        }

    }

    private void broadcastHandshaker() {
        broadcastHandshakeThread = new Thread(() -> {
            try {
                udpHandler.startBroadcastHandshakeListener();
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        });
        broadcastHandshakeThread.start();
    }


    /**
     * Method to handle the "Choose File" button
     **/
    private void onClickChooseFile() {
        askForFilePermission(this, this::chooseFile);
    }

    private void chooseFile() {
        //Intent is an object that allows you to launch the Android file picker to choose a file
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        /*
        Indicates that all files on the device can be selected
            * "image/*" to select only images
            * "application/*" to select only PDF files
        */
        intent.setType("*/*");
        //Adds a filter to the intent object to display only directly openable files
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        //Allows the intent object to be executed and show the results
        filePickerLauncher.launch(intent);
    }


    /**
     * filePickerLauncher is an ActivityResultLauncher<Intent> that handles the result of the file picker activity.
     **/
    private final ActivityResultLauncher<Intent> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                    Uri fileUri = result.getData().getData();
                    if (fileUri != null) {
                        selectedFileUri = fileUri;
                        updateFileChosenButton();
                        return;
                    }
                }
                Toast.makeText(getContext(), "Nessun file selezionato", Toast.LENGTH_SHORT).show();
            });

    void updateFileChosenButton() {
        requireActivity().runOnUiThread(() -> {
            binding.fileChosen.setText((selectedFileUri != null ? "FILE: " + FileUtils.getFileInfoFromUri(requireContext(), selectedFileUri).name : "Select a file"));
            FileThumbnailLoader.loadFileThumbnail(binding.fileChosen.getText().toString(), selectedFileUri, binding, requireActivity(), 105, 105);
        });
    }

    public boolean canFileBeSent() {
        boolean isOptionalMessageValid = !binding.optionalMessage.getText().toString().isEmpty();
        boolean isFileUriValid = selectedFileUri != null;

        return (isOptionalMessageValid || isFileUriValid);
    }
}
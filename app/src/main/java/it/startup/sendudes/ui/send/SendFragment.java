package it.startup.sendudes.ui.send;

import static it.startup.sendudes.utils.IConstants.FILE_TRANSFER_PORT;
import static it.startup.sendudes.utils.IConstants.PING_PORT;
import static it.startup.sendudes.utils.IConstants.RECEIVE_PORT;
import static it.startup.sendudes.utils.IConstants.username;
import static it.startup.sendudes.utils.files_utils.PermissionHandler.askForFilePermission;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import it.startup.sendudes.R;
import it.startup.sendudes.databinding.FragmentSendBinding;
import it.startup.sendudes.utils.file_transfer_utils.TcpClient;
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

    View currentlySelectedView = null;
    String selectedIp = null;


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
        } catch (IOException e) {
            Log.d("SOCKET ERROR", e.getMessage() == null ? "its null" : e.getMessage());
        }
        uiActivityStart();
        broadcastHandshaker();
    }

    @NonNull
    private ArrayAdapter<String> getIpListAdapter(HashMap<String, String> scannedIPs) {
        return new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_dropdown_item_1line,
                new ArrayList<>(scannedIPs.values())
        );
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSendBtnState();
    }

    @Override
    public void onStop() {
        super.onStop();
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

    private void uiActivityStart() {
        binding.btnSend.setEnabled(false);
        binding.twUserIp.setText(username);
        binding.btnPickFile.setOnClickListener(v -> onClickChooseFile());
        isOptionalMessageFieldEmpty();
        binding.btnNetworkScanner.setOnClickListener(v -> {
            binding.btnNetworkScanner.setEnabled(false);

            new Thread(() -> {
                for (int i = 0; i < 32; i++) {
                    udpHandler.scanNetwork();
                }
                binding.btnNetworkScanner.postDelayed(() -> {
                    binding.btnNetworkScanner.setEnabled(true);
                    binding.scanProgressBar.setVisibility(View.GONE);
                    binding.scannedMsg.setText("No user found");
                }, 2000);
                requireActivity().runOnUiThread(() -> {
                    binding.scanProgressBar.setVisibility(View.VISIBLE);
                    binding.scannedMsg.setText("Loading");
                });
            }).start();

        });
        OnListUpdate updatedList = scannedIPs -> {
            requireActivity().runOnUiThread(() -> {
                if (scannedIPs == null) {
                    Toast.makeText(getContext(), "no connection", Toast.LENGTH_LONG).show();

                    return;
                }
                if (scannedIPs.isEmpty()) {
                    binding.scannedMsg.setVisibility(View.VISIBLE);
                    binding.foundIps.setVisibility(View.GONE);
                    Log.d("SCANNED USERS: ", "NO USER FOUND");
                } else {
                    binding.scannedMsg.setVisibility(View.GONE);
                    binding.foundIps.setVisibility(View.VISIBLE);

                    ArrayAdapter<String> ipListContent = getIpListAdapter(scannedIPs);
                    binding.foundIps.setAdapter(ipListContent);
                    binding.btnSend.setEnabled(false);
                    ipListContent.notifyDataSetChanged();


                    binding.foundIps.setOnItemClickListener((adapterView, view, position, id) -> {
                        if (currentlySelectedView != null) {
                            currentlySelectedView.setSelected(false);
                            currentlySelectedView.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                        }

                        view.setSelected(true);
                        view.setBackgroundColor(getResources().getColor(R.color.teal_200));
                        currentlySelectedView = view;
                        selectedIp = (String) adapterView.getItemAtPosition(position);
                        updateSendBtnState();
                    });
                }
            });
        };
        try {
            udpHandler.onListUpdate(updatedList);
        } catch (Exception e) {
            Toast.makeText(getContext(), "no connection", Toast.LENGTH_LONG).show();
        }
        binding.btnSend.setOnClickListener(l -> {
            if (binding.btnSend.isEnabled() && selectedIp != null) {
                TCP_clientThread(selectedIp.split("#", 2)[1]);

                if (currentlySelectedView != null) {
                    currentlySelectedView.setSelected(false);
                    currentlySelectedView.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                    currentlySelectedView = null;
                    selectedIp = null;
                }

                binding.btnSend.setEnabled(false);
            }
        });
        try {
            tcpStarter();
        } catch (Exception e) {
            Toast.makeText(getContext(), "no connection", Toast.LENGTH_LONG).show();
        }
    }

    private void tcpStarter() {
        tcpClient.setTransferSuccessfullEvent(() ->
                requireActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "TRANSFER FINISHED SUCCESSFULLY", Toast.LENGTH_SHORT).show();
                            selectedFileUri = null;
                            updateSendBtnState();
                        }
                ));
        tcpClient.setConnectionBusyEvent(() ->
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "User device is transferring files", Toast.LENGTH_SHORT).show()
                )
        );
        tcpClient.setTransferErrorEvent(() ->
                requireActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "ERROR OCCURRED WHILE TRANSFERRING THE FILE", Toast.LENGTH_SHORT).show();
                            updateSendBtnState();
                        }
                ));
    }

    public void TCP_clientThread(String ip) {
        tcpClientThread = new Thread(() -> tcpClient.sendFileToServer(ip, FILE_TRANSFER_PORT, selectedFileUri, username, binding.optionalMessage.getText().toString(), getContext()));
        tcpClientThread.start();
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


    //Metodo per gestire il Btn "Choose File"
    private void onClickChooseFile() {
        askForFilePermission(this, this::chooseFile);
    }


    private void chooseFile() {
        //intent è un oggetto che permeate di avviare il file picker di android per scegliere un file
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        /*
        Indica che tutti i file del dispositivo possono essere selezionati
        * "image/*" per selezionare solo immagini
        * "application/*" per selezionere solo file PDF
        */
        intent.setType("*/*");
        //aggiunge all'oggetto intent un filtro per visualizzare i file direttamente apribili
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        //permette di eseguire l'oggetto intent, e di mostrare i risultati
        filePickerLauncher.launch(intent);
    }


    /*
     * filePickerLauncher is an ActivityResultLauncher<Intent>
     * that handles the result of the file picker activity.
     */
    private final ActivityResultLauncher<Intent> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                    Uri fileUri = result.getData().getData();
                    if (fileUri != null) {
                        selectedFileUri = fileUri;
                        updateSendBtnState();
                        return;
                    }
                }
                Toast.makeText(getContext(), "Nessun file selezionato", Toast.LENGTH_SHORT).show();
            });

    public void isOptionalMessageFieldEmpty() {
        binding.optionalMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateSendBtnState();
            }
        });
    }

    public void updateSendBtnState() {
        requireActivity().runOnUiThread(() -> {

            binding.fileChosen.setText((selectedFileUri != null ? "File scelto: " + FileUtils.getFileInfoFromUri(getContext(), selectedFileUri).name : "Nessun file selezionato"));
            loadSelectedFileThumbnail();
            boolean isOptionalMessageValid = !binding.optionalMessage.getText().toString().isEmpty();
            boolean isFileUriValid = selectedFileUri != null;
            boolean isIpValid = selectedIp != null && !selectedIp.isEmpty();

            binding.btnSend.setEnabled((isOptionalMessageValid || isFileUriValid) && isIpValid);
        });
    }

    private void loadSelectedFileThumbnail() {
        if (!binding.fileChosen.getText().toString().equalsIgnoreCase("Nessun file selezionato")) {
            try {
                Size mSize = new Size(105, 105);
                CancellationSignal ca = new CancellationSignal();
                Bitmap bitmapThumbnail = requireActivity().getContentResolver().loadThumbnail(selectedFileUri, mSize, ca);
                System.out.println("THUMBNAIL: " + bitmapThumbnail);

                binding.selectedFileThumbnail.setImageBitmap(bitmapThumbnail);
            } catch (Exception e) {
                System.out.println("ERROR CREATING THUMBNAIL: " + e.getMessage());
                if (Objects.requireNonNull(e.getMessage()).contains("audio")) {
                    loadAudioFileThumbnail();
                }
            }
        }
    }

    private void loadAudioFileThumbnail() {
        binding.selectedFileThumbnail.setImageResource(R.drawable.headphones_24px);
        binding.selectedFileThumbnail.setMaxWidth(42);
        binding.selectedFileThumbnail.setMaxHeight(42);
    }
}
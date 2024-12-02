package it.startup.sendudes.ui.send;

import static it.startup.sendudes.utils.IConstants.FILE_TRANSFER_PORT;
import static it.startup.sendudes.utils.IConstants.PING_PORT;
import static it.startup.sendudes.utils.IConstants.RECEIVE_PORT;
import static it.startup.sendudes.utils.IConstants.REQUEST_CODE_READ_WRITE_EXTERNAL_STORAGE;
import static it.startup.sendudes.utils.network_discovery.NetworkUtils.getMyIP;
import static it.startup.sendudes.utils.file_transfer_utils.TCP_Client.clientConnection;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.io.File;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import it.startup.sendudes.R;
import it.startup.sendudes.databinding.FragmentSendBinding;
import it.startup.sendudes.utils.network_discovery.UDP_NetworkUtils;
import it.startup.sendudes.utils.UriToPath;

public class SendFragment extends Fragment {
    //FragmentHomeBinding is a class generated automatically when View Binding is enabled (in the android v8 and later on ig)
    private FragmentSendBinding binding;
    private Thread broadcastHandshakeThread;
    private Thread tcpClientThread;
    private UDP_NetworkUtils udpHandler;
    private boolean permissionsGranted = false; // Flag to track permissions
    private Map.Entry<String, String> entry;
    private String fileName;
    private long fileSize = 0;
    private File fileToSend;

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
            udpHandler = new UDP_NetworkUtils(PING_PORT, RECEIVE_PORT);
        } catch (IOException e) {
            Log.d("SOCKET ERROR", e.getMessage() == null ? "its null" : e.getMessage());
        }

        binding.btnSend.setEnabled(false);
        binding.btnGetIp.setOnClickListener(v -> onClickGetIp());
        binding.btnPickFile.setOnClickListener(v -> onClickChooseFile());

        binding.btnNetworkScanner.setOnClickListener(v -> {
            binding.btnNetworkScanner.setEnabled(false);

            new Thread(() -> {
                for (int i = 0; i < 32; i++) {
                    udpHandler.scanNetwork();
                }
                binding.btnNetworkScanner.postDelayed(() -> binding.btnNetworkScanner.setEnabled(true), 4000);
            }).start();

        });
        udpHandler.onListUpdate((scannedIPs) ->
        {
            requireActivity().runOnUiThread(() -> {
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

                        binding.btnSend.setEnabled(true);
                    });
                }
            });
        });

        broadcastHandshaker();
        binding.btnSend.setOnClickListener(l -> {
            if (binding.btnSend.isEnabled() && selectedIp != null) {
                System.out.println(selectedIp);
                TCP_clientThread(selectedIp);

                if (currentlySelectedView != null) {
                    currentlySelectedView.setSelected(false);
                    currentlySelectedView.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                    currentlySelectedView = null;
                    selectedIp = null;
                }

                binding.btnSend.setEnabled(false);
            }
        });
    }

    @NonNull
    private ArrayAdapter<String> getIpListAdapter(HashMap<String, String> scannedIPs) {
        return new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_dropdown_item_1line,
                new ArrayList<>(scannedIPs.keySet())
        );
    }

    @Override
    public void onStop() {
        super.onStop();
        if (broadcastHandshakeThread != null && broadcastHandshakeThread.isAlive())
            broadcastHandshakeThread.interrupt();
        if (tcpClientThread != null && tcpClientThread.isAlive()) tcpClientThread.interrupt();
        udpHandler.closeSockets();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public void TCP_clientThread(String ip) {
        tcpClientThread = new Thread(() -> {
            clientConnection(ip, FILE_TRANSFER_PORT, fileToSend);
        });
        tcpClientThread.start();
    }

    private void onClickGetIp() {
//       got em 2 ways to access em ui elements usin java, the one right below this line basically uses a traditional way to access the ui elements, and this way doesn't provide type safety, on the other hand the viewModel way does cuz it's binded to the fragment.
//       Button btn = root.findViewById(R.id.clickmebtn);
        binding.twUserIp.setText(getMyIP());
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
        // check se permessi sono abilitati, se no chiede di attivarli
        if (!checkPerms()) {
            askPerms();
        } else {
            chooseFile();
        }
    }

    private boolean checkPerms() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            return permissionsGranted;
        }
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
     * "filePickerLauncher" è un oggetto di tipo ActivityResultLauncher<Intent>per gestire un intento
     * "registerForActivityResult" è un metodo che crea un oggetto "result" passandogli una attività da eseguire con un intent
     * "result.getResultCode()" questo contine un valore per indicare se è andato a buon fine o meno (1-RESULT_OK oppure 2-RESULT_CENCELED)
     * "getData()" contiene l'intent usato e il file se è stato selezionato
     */
    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
            Uri fileUri = result.getData().getData();
            if (fileUri != null) {
                onFileChosen(fileUri);
            }
        } else {
            Toast.makeText(getContext(), "Nessun file selezionato", Toast.LENGTH_SHORT).show();
        }
    });

    //metodo che viene eseguito quando viene selezionato un file
    private void onFileChosen(Uri fileUri) {
        // Aggiorna la TextView con il percorso del file scelto
        String absolutePath = UriToPath.getPathFromUri(getContext(), fileUri);
        File file = getActualFile(absolutePath);
        if (file.exists()) {
            fileToSend = file;
            binding.fileChosen.setText("File scelto: " + absolutePath);
        } else {
            Log.e("FileError", "File does not exist at the specified path: " + absolutePath);
        }
    }

    public void askPerms() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", getActivity().getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
        } else {
            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_CODE_READ_WRITE_EXTERNAL_STORAGE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_READ_WRITE_EXTERNAL_STORAGE:
                if (grantResults.length > 0) {
                    boolean readGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean writeGranted = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                    if (readGranted && writeGranted) {
                        permissionsGranted = true; // tutte e due i perms garantiti
                    } else {
                        permissionsGranted = false; // Almeno uno dei due perms non garantiti
                    }
                } else {
                    permissionsGranted = false; // nessuna interazione dall'utente
                }
                break;
        }
    }

    private File getActualFile(String path) {
        if (!path.isBlank()) {
            File file = new File(path);
            return file;
        }
        return null;
    }


}
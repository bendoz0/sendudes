package it.startup.sendudes.ui.send;

import static it.startup.sendudes.utils.IConstants.FILE_TRANSFER_PORT;
import static it.startup.sendudes.utils.IConstants.MSG_CLIENT_PING;
import static it.startup.sendudes.utils.IConstants.PING_PORT;
import static it.startup.sendudes.utils.IConstants.RECEIVE_PORT;
import static it.startup.sendudes.utils.IConstants.REQUEST_CODE_READ_WRITE_EXTERNAL_STORAGE;
import static it.startup.sendudes.utils.UDP_NetworkUtils.getMyIP;
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
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

import it.startup.sendudes.databinding.FragmentSendBinding;
import it.startup.sendudes.utils.UDP_NetworkUtils;
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

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSendBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        try {
            udpHandler = new UDP_NetworkUtils(RECEIVE_PORT, PING_PORT);
        } catch (IOException e) {
            Log.d("SOCKET ERROR", e.getMessage() == null ? "its null" : e.getMessage());
        }

        binding.btnSend.setEnabled(false);
        binding.btnGetIp.setOnClickListener(v -> onClickGetIp());
        binding.btnPickFile.setOnClickListener(v -> onClickChooseFile());

        binding.btnNetworkScanner.setOnClickListener(v -> {
            onClickScanNetwork();
        });
        broadcastHandshaker();
        binding.btnSend.setOnClickListener(l -> {
            if (binding.btnSend.isEnabled()) {
                System.out.println(entry.getKey());
                TCP_clientThread(entry.getKey());
            }
        });
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
            if (fileName != null && !fileName.isEmpty()) {
                if (fileSize > 0)
                    clientConnection(ip, FILE_TRANSFER_PORT, fileName, fileSize);
            }
        });
        tcpClientThread.start();
    }
    public void onClickScanNetwork(){
        udpHandler.broadcast(MSG_CLIENT_PING);
        if (udpHandler.getFoundIps().isPresent()) {
            binding.foundIps.setText(udpHandler.getFoundIps().get().toString());
            entry = udpHandler.getFoundIps().get().entrySet().iterator().next();
            binding.btnSend.setEnabled(true);
        } else {
            binding.foundIps.setText("No user found");
        }
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
        if (!file.exists()) {
            Log.e("FileError", "File does not exist at the specified path: " + absolutePath);
            return;
        } else {
            try {
                fileSize = file.length();
                fileName = file.getName();
                BufferedInputStream fiS = new BufferedInputStream(new FileInputStream(file));
                byte[] buffer = new byte[1024];
                int bytesRead = 0;
                Integer count = 0;
                while ((bytesRead = fiS.read(buffer)) != -1) {
//                    Log.d("BYTE READ", buffer.toString());
                    count++;
                }
                Log.d("BYTES READ", count.toString());
                fiS.close();
//                String fileData = new String(buffer);
                binding.fileChosen.setText("File scelto: " + absolutePath);
            } catch (Exception e) {
                binding.fileChosen.setText("File scelto " + e.getMessage());
            }
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
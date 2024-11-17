package it.startup.sendudes.ui.send;

import static it.startup.sendudes.utils.NetworkUtils.MSG_CLIENT_PING;
import static it.startup.sendudes.utils.NetworkUtils.PING_PORT;
import static it.startup.sendudes.utils.NetworkUtils.RECEIVE_PORT;
import static it.startup.sendudes.utils.NetworkUtils.broadcast;
import static it.startup.sendudes.utils.NetworkUtils.broadcastHandshake;
import static it.startup.sendudes.utils.NetworkUtils.getFoundIps;
import static it.startup.sendudes.utils.NetworkUtils.getMyIP;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.net.DatagramSocket;
import java.net.SocketException;


import it.startup.sendudes.databinding.FragmentSendBinding;

public class SendFragment extends Fragment {
    //FragmentHomeBinding is a class generated automatically when View Binding is enabled (in the android v8 and later on ig)
    private FragmentSendBinding binding;
    private Thread broadcastHandshakeThread;
    private DatagramSocket socket;
    private DatagramSocket listenerSocket;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentSendBinding.inflate(inflater, container, false);

        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        try {
            socket = new DatagramSocket(PING_PORT);
            listenerSocket = new DatagramSocket(RECEIVE_PORT);
        } catch (SocketException e) {
            Log.d("SOCKET ERROR", e.getMessage() == null ? "its null" : e.getMessage());
        }


        binding.btnGetIp.setOnClickListener(v -> onClickGetIp());
        binding.btnPickFile.setOnClickListener(v -> onClickChooseFile());
        binding.btnNetworkScanner.setOnClickListener(v -> {
            broadcast(socket, MSG_CLIENT_PING);
            binding.foundIps.setText(getFoundIps().toString());
        });
        broadcastHandshaker(listenerSocket);
        Log.d("MESSAGE: ", "STARTED SUCCESSFULLY: " + socket.isClosed());
    }

    @Override
    public void onStop() {
        super.onStop();
        if (broadcastHandshakeThread != null && broadcastHandshakeThread.isAlive())
            broadcastHandshakeThread.interrupt();
        if (!socket.isClosed()) socket.close();
        if (!listenerSocket.isClosed()) listenerSocket.close();

        Log.d("MESSAGE: ", "CLOSED SUCCESSFULLY: " + socket.isClosed());

    }

    private void onClickGetIp() {
//       got em 2 ways to access em ui elements usin java, the one right below this line basically uses a traditional way to access the ui elements, and this way doesn't provide type safety, on the other hand the viewModel way does cuz it's binded to the fragment.
//       Button btn = root.findViewById(R.id.clickmebtn);
        binding.twUserIp.setText(getMyIP());
    }

    private void broadcastHandshaker(DatagramSocket socket) {
        broadcastHandshakeThread = new Thread(() -> {
            try {
                broadcastHandshake(socket, listenerSocket);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        });
        broadcastHandshakeThread.start();
    }


    //Metodo per gestire il Btn "Choose File"
    private void onClickChooseFile() {
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
     * "filePickerLauncher" variabile di tipo ActivityResultLauncher<Intent>per gestire un intento
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
        binding.fileChosen.setText("File scelto: " + fileUri.toString());
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
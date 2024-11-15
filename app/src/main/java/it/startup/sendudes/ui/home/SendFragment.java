package it.startup.sendudes.ui.home;

import android.os.Bundle;
import android.os.StrictMode;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;


import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import it.startup.sendudes.databinding.FragmentHomeBinding;

public class SendFragment extends Fragment {
    //FragmentHomeBinding is a class generated automatically when View Binding is enabled (in the android v8 and later on ig)
    private FragmentHomeBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        SendViewModel sendViewModel =
                new ViewModelProvider(this).get(SendViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
//        getMyIP();
        final TextView textView = binding.textHome;
        testBtn();
        sendViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

    private void testBtn() {

//       got em 2 ways to access em ui elements usin java, the one right below this line basically uses a traditional way to access the ui elements, and this way doesn't provide type safety, on the other hand the viewModel way does cuz it's binded to the fragment.
//       Button btn = root.findViewById(R.id.clickmebtn);

        final Button btn = binding.clickmebtn;
        btn.setOnClickListener(v -> getMyIP());
    }


    private void getMyIP(){
            try {
                InetAddress localAddress = null;

                Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                while (interfaces.hasMoreElements()) {
                    NetworkInterface networkInterface = interfaces.nextElement();

                    if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                        continue;
                    }

                    Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress address = addresses.nextElement();

                        if (!address.isLoopbackAddress() && address instanceof java.net.Inet4Address) {
                            localAddress = address;
                            break;
                        }
                    }

                    if (localAddress != null) {
                        break;
                    }
                }

                if (localAddress != null) {
                    System.out.println("Local IP Address: " + localAddress.getHostAddress());
                } else {
                    System.out.println("No non-loopback address found.");
                }

            } catch (SocketException e) {
                System.err.println("Error while getting the network interfaces: " + e.getMessage());
            }

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
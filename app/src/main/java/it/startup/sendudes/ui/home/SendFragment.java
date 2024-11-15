package it.startup.sendudes.ui.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;


import org.w3c.dom.Text;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import it.startup.sendudes.databinding.FragmentSendBinding;

public class SendFragment extends Fragment {
    //FragmentHomeBinding is a class generated automatically when View Binding is enabled (in the android v8 and later on ig)
    private FragmentSendBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentSendBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        binding.clickmebtn.setOnClickListener(v -> onClickGetIp());
        return root;
    }

    @Override
    public void onResume(){
        super.onResume();
    }

    private void onClickGetIp() {
//       got em 2 ways to access em ui elements usin java, the one right below this line basically uses a traditional way to access the ui elements, and this way doesn't provide type safety, on the other hand the viewModel way does cuz it's binded to the fragment.
//       Button btn = root.findViewById(R.id.clickmebtn);

        binding.textHome.setText(getMyIP());
    }



    private String getMyIP() {
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
                return localAddress.getHostAddress();
            } else {
                System.out.println("No non-loopback address found.");
            }

        } catch (SocketException e) {
            System.err.println("Error while getting the network interfaces: " + e.getMessage());
        }
        return "Cant find IP";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
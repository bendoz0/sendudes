package it.startup.sendudes.utils.network_discovery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class NetworkConnectivityManager {
    private Context context;
    private OnIPChangedListener listener;
    private ConnectivityManager connectivityManager;
    private NetworkCallback networkCallback;

    public NetworkConnectivityManager(Context context, OnIPChangedListener listener) {
        this.context = context;
        this.listener = listener;
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public void startListening() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            networkCallback = new NetworkCallback();
            connectivityManager.registerDefaultNetworkCallback(networkCallback);
        } else {
            registerLegacyNetworkReceiver();
        }
        updateNetworkInfo();
    }

    public void stopListening() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
    }

    private class NetworkCallback extends ConnectivityManager.NetworkCallback {
        @Override
        public void onAvailable(Network network) {
            updateNetworkInfo();
        }

        @Override
        public void onLost(Network network) {
            if (listener != null) {
                listener.onNoNetwork();
            }
        }
    }

    private void registerLegacyNetworkReceiver() {
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateNetworkInfo();
            }
        }, filter);
    }

    private void updateNetworkInfo() {
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnected()) {
            String ip = getCurrentIPAddress();
            if (ip != null && !ip.isEmpty() && !ip.equals("Cant find IP")) {
                if (listener != null) {
                    listener.onNetworkChanged(ip);
                }
            }
        } else {
            if (listener != null) {
                listener.onNoNetwork();
            }
        }
    }

    public static String getCurrentIPAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();

                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }

                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();

                    if (!address.isLoopbackAddress() && address instanceof Inet4Address) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            Log.e("NetworkUtils", "Error while getting network interfaces: " + e.getMessage());
        }
        return "Cant find IP";
    }


}

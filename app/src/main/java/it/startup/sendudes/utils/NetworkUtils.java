package it.startup.sendudes.utils;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class NetworkUtils {
    private static DatagramSocket socket;

    public static void tryBroadcast() {
        try {
            socket = new DatagramSocket();
            socket.setBroadcast(true);

            byte[] buffer = ("HELLO FROM: " + getMyIP()).getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName("255.255.255.255"), 8000);
            socket.send(packet);
            Log.d("BROADCAST", "DONE");
        } catch (IOException e) {
            Log.d("BROADCAST ERROR", e.getMessage() == null ? "ERROR NULL" : e.getMessage());
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }

    public static void broadcast() {
        new Thread(NetworkUtils::tryBroadcast).start();
    }

    public static String getMyIP() {
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
}

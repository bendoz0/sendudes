package it.startup.sendudes.utils;

import static it.startup.sendudes.utils.IConstants.MSG_CLIENT_NOT_RECEIVING;
import static it.startup.sendudes.utils.IConstants.MSG_CLIENT_PING;
import static it.startup.sendudes.utils.IConstants.MSG_CLIENT_RECEIVING;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Optional;

public class UDP_NetworkUtils {

    private static final HashMap<String, String> foundIps = new HashMap<>();

    public static void tryBroadcast(DatagramSocket socket, String message) {
        try {
            socket.setBroadcast(true);
            byte[] buffer = !message.isEmpty() ? message.getBytes() : ("HELLO FROM: " + getMyIP()).getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName("255.255.255.255"), socket.getLocalPort());
            socket.send(packet);
            Log.d("BROADCAST", "BROADCASTED MSG: " + message + " PORT: " + socket.getLocalPort());
        } catch (IOException e) {
            Log.d("BROADCAST ERROR", e.getMessage() == null ? "receiver socket is null" : "receiver " + e.getMessage());
        }
    }

    public static void broadcast(DatagramSocket socket, String message) {
        Thread x = new Thread(() -> tryBroadcast(socket, message));
        x.start();
        try {
            x.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
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
                return localAddress.getHostAddress();
            } else {
                System.out.println("No non-loopback address found.");
            }

        } catch (SocketException e) {
            System.err.println("Error while getting the network interfaces: " + e.getMessage());
        }
        return "Cant find IP";
    }

    public static void broadcastHandshake(DatagramSocket socket, DatagramSocket listenerSocket) {
        try {
            byte[] buffer = new byte[2048];

            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            while (true) {
                listenerSocket.receive(packet);

                String msg = new String(buffer, 0, packet.getLength());
                Log.d("RECEIVE: ", packet.getAddress().getHostName() + ": " + msg);
                handleReceivedPacket(packet, socket);
                packet.setLength(buffer.length);
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    public static Optional<HashMap<String, String>> getFoundIps() {
        if (!foundIps.isEmpty()) return Optional.of(foundIps);
        return Optional.empty();
    }

    // TODO: MAKE USER REMOVER EFFICIENT
    private static void handleReceivedPacket(DatagramPacket receivedPack, DatagramSocket socket) {
        String ip = receivedPack.getAddress().getHostAddress();
        if (ip == null) {
            return;
        }
        String hostName = receivedPack.getAddress().getHostName();
        String msg = new String(receivedPack.getData(), 0, receivedPack.getLength());

        switch (msg) {
            case MSG_CLIENT_PING:
                broadcast(socket, MSG_CLIENT_RECEIVING);
                break;
            case MSG_CLIENT_NOT_RECEIVING:
                foundIps.remove(ip);
                break;
            case MSG_CLIENT_RECEIVING:
                if (!foundIps.containsKey(ip) && !ip.contains(getMyIP())) {
                    System.out.println("ADDED NEW IP: " + ip);
                    foundIps.put(ip, hostName);
                }
                break;
        }
    }


}

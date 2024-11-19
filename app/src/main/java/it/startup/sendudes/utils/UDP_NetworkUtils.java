package it.startup.sendudes.utils;

import static it.startup.sendudes.utils.IConstants.MSG_CLIENT_NOT_RECEIVING;
import static it.startup.sendudes.utils.IConstants.MSG_CLIENT_PING;
import static it.startup.sendudes.utils.IConstants.MSG_CLIENT_RECEIVING;
import static it.startup.sendudes.utils.IConstants.MULTICAST_ADDRESS;
import static it.startup.sendudes.utils.NetworkUtils.getMyIP;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.HashMap;
import java.util.Optional;

public class UDP_NetworkUtils {
    private final DatagramSocket sendSocket;
    private final MulticastSocket listenerSocket;
    private final HashMap<String, String> foundIps = new HashMap<>();

    public UDP_NetworkUtils(int sendSocketPort, int listenerSocketPort) throws IOException {

        this.sendSocket = new DatagramSocket(sendSocketPort);
//        sendSocket.joinGroup(group);

        this.listenerSocket = new MulticastSocket(listenerSocketPort);
        InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
        listenerSocket.joinGroup(group);
    }

    public void tryBroadcast(String message) {
        try {
            sendSocket.setBroadcast(true);
            byte[] buffer = !message.isEmpty() ? message.getBytes() : ("HELLO FROM: " + getMyIP()).getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(MULTICAST_ADDRESS), sendSocket.getLocalPort());
            sendSocket.send(packet);
            Log.d("BROADCAST", "BROADCASTED MSG: " + message);
        } catch (IOException e) {
            Log.d("BROADCAST ERROR", e.getMessage() == null ? "receiver socket is null" : "receiver " + e.getMessage());
        }
    }

    public void broadcast(String message) {
        Thread x = new Thread(() -> tryBroadcast(message));
        x.start();
        try {
            x.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void startBroadcastHandshakeListener() {
        try {
            byte[] buffer = new byte[2048];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(MULTICAST_ADDRESS), listenerSocket.getLocalPort());

            while (true) {
                listenerSocket.receive(packet);
                String msg = new String(buffer, 0, packet.getLength());
                Log.d("RECEIVE: ", packet.getAddress().getHostName() + ": " + msg);
                handleReceivedPacket(packet);
                packet.setLength(buffer.length);
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    public Optional<HashMap<String, String>> getFoundIps() {
        if (!foundIps.isEmpty()) return Optional.of(foundIps);
        return Optional.empty();
    }

    // TODO: MAKE USER REMOVER EFFICIENT
    private void handleReceivedPacket(DatagramPacket receivedPack) {
        String ip = receivedPack.getAddress().getHostAddress();
        if (ip == null) {
            return;
        }
        String hostName = receivedPack.getAddress().getHostName();
        String msg = new String(receivedPack.getData(), 0, receivedPack.getLength());

        switch (msg) {
            case MSG_CLIENT_PING:
                broadcast(MSG_CLIENT_RECEIVING);
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
    public void closeSockets(){
        sendSocket.close();
        listenerSocket.close();
    }

}

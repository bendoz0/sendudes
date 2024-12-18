package it.startup.sendudes.utils.network_discovery;

import static it.startup.sendudes.utils.IConstants.MSG_CLIENT_NOT_RECEIVING;
import static it.startup.sendudes.utils.IConstants.MSG_CLIENT_PING;
import static it.startup.sendudes.utils.IConstants.MSG_CLIENT_RECEIVING;
import static it.startup.sendudes.utils.IConstants.MULTICAST_ADDRESS;
import static it.startup.sendudes.utils.IConstants.username;
import static it.startup.sendudes.utils.network_discovery.NetworkConnectivityManager.getCurrentIPAddress;

import android.util.Log;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.HashMap;

public class UDP_NetworkUtils {
    private final DatagramSocket sendSocket;
    private final MulticastSocket listenerSocket;
    private final HashMap<String, String> foundIps = new HashMap<>();
    private OnListUpdate actionListUpdated;

    public UDP_NetworkUtils(int sendSocketPort, int listenerSocketPort) throws IOException {
        closeSockets();
        this.sendSocket = new DatagramSocket(sendSocketPort);
        this.listenerSocket = new MulticastSocket(listenerSocketPort);
        InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
        listenerSocket.joinGroup(group);
    }

    public void tryBroadcast(String message) {
        try {
            sendSocket.setBroadcast(true);
            byte[] buffer = !message.isEmpty() ? (username() + ":" + message).getBytes() : ("HELLO FROM: " + getCurrentIPAddress()).getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(MULTICAST_ADDRESS), sendSocket.getLocalPort());
            sendSocket.send(packet);
            Log.d("BROADCAST", "BROADCASTED MSG: " + message);
        } catch (IOException e) {
            Log.d("BROADCAST ERROR", e.getMessage() == null ? "receiver socket is null" : "receiver " + e.getMessage());
        }
    }

    public void broadcast(String message) throws RuntimeException {
        Thread x = new Thread(() -> tryBroadcast(message));
        x.start();
        try {
            x.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void scanNetwork() {
        broadcast(MSG_CLIENT_PING);
        _triggerListUpdateEvent();
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
            System.err.println("UDP: " + e.getMessage());
        }
    }


    public void onListUpdate(OnListUpdate x) {
        actionListUpdated = x;
    }

    private void _triggerListUpdateEvent() {
        if (actionListUpdated != null) {
            actionListUpdated.listUpdated(foundIps);
        }
    }

    // TODO: MAKE USER REMOVER EFFICIENT
    private void handleReceivedPacket(DatagramPacket receivedPack) {
        String ip = receivedPack.getAddress().getHostAddress();
        if (ip == null) {
            return;
        }
        String msg = new String(receivedPack.getData(), 0, receivedPack.getLength());
        String[] arrayedMsg = msg.split(":", 2);

        switch (arrayedMsg[1]) {
            case MSG_CLIENT_PING:
                broadcast(MSG_CLIENT_RECEIVING);
                break;

            case MSG_CLIENT_NOT_RECEIVING:
                foundIps.remove(ip);
                Log.d("RECEIVED PACKET", "handleReceivedPacket: REMOVED IP");
                _triggerListUpdateEvent();
                break;

            case MSG_CLIENT_RECEIVING:
                if (!ip.contains(arrayedMsg[0])) {
                    System.out.println("ADDED NEW IP: " + arrayedMsg[0]+ ip);
                    foundIps.put(ip, arrayedMsg[0]);
                    _triggerListUpdateEvent();
                }
                break;

            default:
                Log.d("RECEIVED PACKET", "handleReceivedPacket: UNKNOWN MESSAGE " +msg);
        }
    }
    public void closeSockets() {
        if (listenerSocket != null) {
            listenerSocket.close();
        }
        if (sendSocket != null) {
            sendSocket.close();
        }
    }
}

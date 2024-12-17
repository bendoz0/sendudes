package it.startup.sendudes.utils.file_transfer_utils.tcp_events;

import it.startup.sendudes.utils.file_transfer_utils.FileTransferPacket;

@FunctionalInterface
public interface OnClientConnected {
    void onConnected(FileTransferPacket fileTransferRequestDetails);
}

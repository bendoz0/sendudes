package it.startup.sendudes.utils.file_transfer_utils.tcp_events;

@FunctionalInterface
public interface OnTransferError {
    void OnTransferFailed();
}

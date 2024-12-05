package it.startup.sendudes.utils;

public interface IConstants {
    public static final int REQUEST_CODE_READ_WRITE_EXTERNAL_STORAGE = 1;
    int PING_PORT = 8000;
    int RECEIVE_PORT = 8001;
    int FILE_TRANSFER_PORT = 8002;
    String MULTICAST_ADDRESS = "224.0.0.167";
    String MSG_CLIENT_NOT_RECEIVING = "NOT_RECEIVING";
    String MSG_CLIENT_PING = "PING";
    String MSG_CLIENT_RECEIVING = "RECEIVING";
    String MSG_ACCEPT_CLIENT = "ACCEPT";
    String MSG_REJECT_CLIENT = "REJECT";
    String MSG_BUSY_CLIENT = "BUSY";
    String MSG_FILETRANSFER_FINISHED = "FILE_RECEIVED";
}

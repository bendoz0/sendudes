package it.startup.sendudes.utils.file_transfer_utils;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

public class FileTransferPacket {
    private String userName;
    private String fileName;
    private long fileSize;
    private String optionalMessage;
    private static final Gson gson = new Gson();

    public FileTransferPacket(String userName, String fileName, long fileSize, String optionalMessage) {
        this.userName = userName;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.optionalMessage = optionalMessage;
    }

    public static String toJson(FileTransferPacket ft) {
        return gson.toJson(ft);
    }

    public static FileTransferPacket fromJson(String str) {
        return gson.fromJson(str, FileTransferPacket.class);
    }

    public String getUserName() {
        return userName;
    }

    public String getOptionalMessage() {
        return optionalMessage;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    @NonNull
    public String toString() {
        return "Username: " + this.userName + "\nFile: " + this.fileName + "\nSize: " + fileSize + " Bytes" + (!optionalMessage.isEmpty() ? "\nMessage: " + optionalMessage : "");
    }
}

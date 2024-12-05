package it.startup.sendudes.utils.file_transfer_utils;

import com.google.gson.Gson;

public class FileTransferPacket {
    private String userName;
    private String fileName;
    private long fileSize;
    private static final Gson gson = new Gson();
    public FileTransferPacket(String userName, String fileName, long fileSize) {
        this.userName = userName;
        this.fileName = fileName;
        this.fileSize = fileSize;
    }

    public static String toJson(FileTransferPacket ft){
        return gson.toJson(ft);
    }

    public static FileTransferPacket fromJson(String str){
        return gson.fromJson(str, FileTransferPacket.class);
    }

    public String getUserName(){
        return userName;
    }
    public String getFileName(){
        return fileName;
    }
    public long getFileSize(){
        return fileSize;
    }
    public String toString(){
        return "Username: " + this.userName + "\nFile: " + this.fileName + "\nSize: " + fileSize + " Bytes";
    }
}

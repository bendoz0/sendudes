package it.startup.sendudes.utils.file_transfer_utils;

public class FileTransferPacket {
    private String userName;
    private String fileName;
    private Double fileSize;

    public FileTransferPacket(String userName, String fileName, Double fileSize) {
        this.userName = userName;
        this.fileName = fileName;
        this.fileSize = fileSize;
    }

    public String toJson(){
        return "{"+
                "userName: " + this.userName + ", " +
                "fileName: " + this.fileName + ", " +
                "fileSize: " + this.fileSize +
                "}";
    }
}

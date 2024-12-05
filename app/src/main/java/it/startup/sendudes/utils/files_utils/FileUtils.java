package it.startup.sendudes.utils.files_utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class FileUtils {
    public static FileInfo getFileInfoFromUri(Context context, Uri uri) {
        String fileName = null;
        long fileSize = 0;

        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(uri, null, null, null, null);

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);

                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex);
                    }
                    if (sizeIndex != -1) {
                        fileSize = cursor.getLong(sizeIndex);
                    }
                }
            } finally {
                cursor.close(); // Ensure the cursor is closed to avoid memory leaks
            }
        }

        return new FileInfo(fileName, fileSize);
    }

    public static class FileInfo {
        public String name;
        public long size;

        public FileInfo(String name, long size) {
            this.name = name;
            this.size = size;
        }
    }
    public static byte[] readBytesFromUri(Context context, Uri uri) {
        // Open a ParcelFileDescriptor from the URI
        ParcelFileDescriptor parcelFileDescriptor = null;
        try {
            parcelFileDescriptor = context.getContentResolver().openFileDescriptor(uri, "r");
        } catch (FileNotFoundException e) {
            return null;
        }
        if (parcelFileDescriptor == null) {
            return null;
        }

        // Get the FileDescriptor and create a FileInputStream
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        FileInputStream fileInputStream = new FileInputStream(fileDescriptor);

        // Read bytes into a byte array
        byte[] data = new byte[(int) parcelFileDescriptor.getStatSize()];

        int bytesRead = 0;
        try {
            bytesRead = fileInputStream.read(data);

            // Close resources
            fileInputStream.close();
            parcelFileDescriptor.close();

            // Check if all bytes were read
            if (bytesRead != data.length) {
                return null;
            }
            return data; // Return the byte array
        } catch (IOException e) {
            return null;
        }
    }
}

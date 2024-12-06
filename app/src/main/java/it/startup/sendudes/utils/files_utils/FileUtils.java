package it.startup.sendudes.utils.files_utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.FileNotFoundException;
import java.io.InputStream;

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

    public static InputStream getFileInputStreamFromURI(Context context, Uri uri) {
        try {
            return context.getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException e) {
            return null;
        }
    }
}

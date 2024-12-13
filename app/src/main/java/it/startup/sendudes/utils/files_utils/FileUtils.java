package it.startup.sendudes.utils.files_utils;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.CancellationSignal;
import android.provider.OpenableColumns;
import android.util.Log;
import android.util.Size;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import it.startup.sendudes.R;
import it.startup.sendudes.databinding.FragmentSendBinding;

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

    public static void loadFileThumbnail(String selectedFile, Uri selectedFileUri, FragmentSendBinding binding, Activity activity, int width, int height) {

        if (!selectedFile.equalsIgnoreCase("Select a file")) {
            try {
                Size mSize = new Size(width, height);
                CancellationSignal ca = new CancellationSignal();
                Bitmap bitmapThumbnail = activity.getContentResolver().loadThumbnail(selectedFileUri, mSize, ca);
                binding.selectedFileThumbnail.setImageBitmap(bitmapThumbnail);
            } catch (Exception e) {
                loadFallBackThumbnail(selectedFile, selectedFileUri, binding, activity);
            }
        }
    }

    private static void loadFallBackThumbnail(String selectedFile, Uri selectedFileUri, FragmentSendBinding binding, Activity activity) {

        if (selectedFile.endsWith("mp3") || selectedFile.endsWith("wav") || selectedFile.endsWith("ooc")) {
            binding.selectedFileThumbnail.setImageResource(R.drawable.headphones_24px);

        } else if (selectedFile.endsWith("pdf") || selectedFile.endsWith("txt")) {
            binding.selectedFileThumbnail.setImageResource(android.R.drawable.ic_menu_save);

        }else if (selectedFile.endsWith("mov") || selectedFile.endsWith("mp4") || selectedFile.endsWith("mkv")){
            loadVideoThumbnailForOlderAPIs(selectedFileUri, binding, activity);

        }else if (selectedFile.endsWith("apk")){
            binding.selectedFileThumbnail.setImageResource(android.R.drawable.sym_def_app_icon);
        }else
            binding.selectedFileThumbnail.setImageResource(R.drawable.baseline_broken_image);

    }

    private static void loadVideoThumbnailForOlderAPIs(Uri selectedFileUri, FragmentSendBinding binding, Activity activity) {
        try (MediaMetadataRetriever retriever = new MediaMetadataRetriever()) {
            retriever.setDataSource(activity, selectedFileUri);
            binding.selectedFileThumbnail.setImageBitmap(retriever.getFrameAtTime());
        } catch (Exception e) {
            Log.d("Video Thumbnail Loader", Objects.requireNonNull(e.getMessage()));
        }
    }

}

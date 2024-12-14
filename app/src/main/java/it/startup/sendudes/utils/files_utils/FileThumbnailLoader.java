package it.startup.sendudes.utils.files_utils;

import android.app.Activity;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.CancellationSignal;
import android.util.Log;
import android.util.Size;

import java.io.IOException;
import java.util.Objects;

import it.startup.sendudes.R;
import it.startup.sendudes.databinding.FragmentSendBinding;

public class FileThumbnailLoader {
    public static void loadFileThumbnail(String selectedFile, Uri selectedFileUri, FragmentSendBinding binding, Activity activity, int width, int height) {

        if (!selectedFile.equalsIgnoreCase("Select a file")) {
            try {
                binding.selectedFileThumbnail.setImageBitmap(simplyLoadBitmap(selectedFileUri, activity, width, height));
            } catch (Exception e) {
                loadFallBackThumbnail(selectedFile, selectedFileUri, binding, activity);
            }
        }
    }

    public static Bitmap simplyLoadBitmap(Uri selectedFileUri, Activity activity, int width, int height) throws IOException {
        Size mSize = new Size(width, height);
        CancellationSignal ca = new CancellationSignal();
        return activity.getContentResolver().loadThumbnail(selectedFileUri, mSize, ca);
    }

    private static void loadFallBackThumbnail(String selectedFile, Uri selectedFileUri, FragmentSendBinding binding, Activity activity) {

        if (selectedFile.endsWith("mp3") || selectedFile.endsWith("wav") || selectedFile.endsWith("aiff")) {
            binding.selectedFileThumbnail.setImageResource(R.drawable.headphones_24px);

        } else if (selectedFile.endsWith("pdf") || selectedFile.endsWith("txt")) {
            binding.selectedFileThumbnail.setImageResource(android.R.drawable.ic_menu_save);

        } else if (selectedFile.endsWith("mov") || selectedFile.endsWith("mp4") || selectedFile.endsWith("mkv")) {
            Bitmap videoThumbnail = loadVideoThumbnailForOlderAPIs(selectedFileUri, activity);
            if (videoThumbnail != null)
                binding.selectedFileThumbnail.setImageBitmap(videoThumbnail);

        } else if (selectedFile.endsWith("apk")) {
            binding.selectedFileThumbnail.setImageResource(android.R.drawable.sym_def_app_icon);
        } else
            binding.selectedFileThumbnail.setImageResource(R.drawable.baseline_broken_image);

    }

    public static Bitmap loadVideoThumbnailForOlderAPIs(Uri selectedFileUri, Activity activity) {
        try (MediaMetadataRetriever retriever = new MediaMetadataRetriever()) {
            retriever.setDataSource(activity, selectedFileUri);
            return retriever.getFrameAtTime();
        } catch (Exception e) {
            Log.d("Video Thumbnail Loader", Objects.requireNonNull(e.getMessage()));
        }
        return null;
    }

}

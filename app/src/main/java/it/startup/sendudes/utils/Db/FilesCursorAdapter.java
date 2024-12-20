package it.startup.sendudes.utils.Db;


import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Objects;

import it.startup.sendudes.R;
import it.startup.sendudes.utils.files_utils.FileThumbnailLoader;

public class FilesCursorAdapter extends CursorAdapter {
    private Activity activity;

    private static class ViewHolder {
        public ImageView image;
        public TextView fileName;
        public TextView fileSize;
        public TextView fileTimeStamp;
        public TextView fileSent;

        public ViewHolder(View view) {
            image = view.findViewById(R.id.rowitem_image);
            fileName = view.findViewById(R.id.rowitem_name);
            fileSize = view.findViewById(R.id.rowitem_size);
            fileTimeStamp = view.findViewById(R.id.rowitem_time_stamp);
            fileSent = view.findViewById(R.id.rowitem_sent);
        }
    }

    public FilesCursorAdapter(Context context, Cursor cursor, Activity activity) {
        super(context, cursor, 0);
        this.activity = activity;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.rowitem_history, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder holder;
        if (view.getTag() == null) {
            holder = new ViewHolder(view);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        String fName = cursor.getString(1);
        String fSize = cursor.getString(2);
        String fTimeStamp = cursor.getString(3);
        String fSent = cursor.getString(4);
        String uri = cursor.getString(5);

        holder.fileName.setText(fName);
        holder.fileSize.setText(fSize);
        holder.fileTimeStamp.setText(fTimeStamp);
        holder.fileSent.setText(fSent.equals("1") ? "File Sent" : "File Received");
        try {
            loadSelectedFileThumbnail(Uri.parse(uri), holder);
        }catch (Exception e){
            String errorMessage = e.getMessage() != null ? e.getMessage() : "aint workinnnnnnnn";
            Log.d("Error Loading Thumbnail", errorMessage);
        }
    }

    private void loadSelectedFileThumbnail(Uri uri, ViewHolder holder) {
        try {
            holder.image.setImageBitmap(FileThumbnailLoader.simplyLoadBitmap(uri, activity, 105, 105));
        } catch (Exception e) {
            Log.d("Error Loading Thumbnail", Objects.requireNonNull(e.getMessage()));

            String fileName = String.valueOf(holder.fileName);
            Log.d("File name", fileName);

            if (fileName.endsWith("mp3") || fileName.endsWith("wav") || fileName.endsWith("aiff")) {
                holder.image.setImageResource(R.drawable.headphones_24px);

            } else if (fileName.endsWith("pdf") || fileName.endsWith("txt")) {
                holder.image.setImageResource(android.R.drawable.ic_menu_save);

            } else if (fileName.endsWith("mov") || fileName.endsWith("mp4") || fileName.endsWith("mkv")) {
                Bitmap videoThumbnail = FileThumbnailLoader.loadVideoThumbnailForOlderAPIs(uri, activity);
                if (videoThumbnail != null)
                    holder.image.setImageBitmap(videoThumbnail);

            } else if (fileName.endsWith("apk")) {
                holder.image.setImageResource(android.R.drawable.sym_def_app_icon);
            } else
                holder.image.setImageResource(R.drawable.baseline_broken_image);

        }
    }

}

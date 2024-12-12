package it.startup.sendudes.utils.Db;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.CancellationSignal;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.Objects;

import it.startup.sendudes.R;

public class FilesCursorAdapter extends CursorAdapter {
    private Activity activity;
    private static  class ViewHolder{
        public ImageView image;
        public TextView fileName;
        public TextView fileSize;
        public TextView fileTimeStamp;
        public TextView fileSent;
        public ViewHolder(View view){
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
        if (view.getTag() == null){
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
        loadSelectedFileThumbnail(Uri.parse(uri), holder);
    }

    private void loadSelectedFileThumbnail(Uri uri, ViewHolder holder) {
        try {
            Size mSize = new Size(105, 105);
            CancellationSignal ca = new CancellationSignal();
            Bitmap bitmapThumbnail = activity.getContentResolver().loadThumbnail(uri, mSize, ca);
            System.out.println("THUMBNAIL: " + bitmapThumbnail);
            holder.image.setImageBitmap(bitmapThumbnail);

        } catch (Exception e) {
            System.out.println("ERROR CREATING THUMBNAIL: " + e.getMessage());
            holder.image.setImageResource(R.drawable.baseline_broken_image);
        }
    }

}

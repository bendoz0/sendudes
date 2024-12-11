package it.startup.sendudes.ui.history;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Environment;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.io.File;
import java.util.Objects;

import it.startup.sendudes.databinding.FragmentHistoryBinding;
import it.startup.sendudes.utils.Db.FilesDbAdapter;

public class HistoryFragment extends Fragment {

    private FragmentHistoryBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        SettingsViewModel notificationsViewModel =
                new ViewModelProvider(this).get(SettingsViewModel.class);

        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        FilesDbAdapter fda = new FilesDbAdapter(getContext()).open();

        Cursor cursor = fda.fetchAllFiles();
        ArrayAdapter<String> adapter = getFilesAdapter(cursor);
        binding.historyList.setAdapter(adapter);
        fda.close();
        return root;
    }

    private ArrayAdapter<String> getFilesAdapter(Cursor cursor){
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1);
        if (cursor.moveToFirst()) {
            do {
                String fileDetails = "";
                int columns = cursor.getColumnCount();
                for (int i = 1; i < columns; i++){
                    switch (i){
                        case 1:
                            fileDetails += "File name: " + cursor.getString(i) + "\n";
                            break;
                        case 2:
                            fileDetails += "Size: " + cursor.getString(i) + "\n";
                            break;
                        case 3:
                            fileDetails += "time stamp: " + cursor.getString(i) + "\n";
                            break;
                        case 4:
                            fileDetails += (cursor.getInt(i) == 1 ? "File sent" : "File received") + "\n";
                            break;
                        case 5:
                            fileDetails += "uri: " + cursor.getString(i) + "\n";
                            loadSelectedFileThumbnail(Uri.parse(cursor.getString(i)));
                            break;
                        default:
                            fileDetails += cursor.getString(i) + "\n";
                            break;
                    }
                }
                adapter.add(fileDetails);
            } while (cursor.moveToNext());
        }
        return adapter;
    }


    private void loadSelectedFileThumbnail(Uri uri) {
            try {
                Size mSize = new Size(105, 105);
                CancellationSignal ca = new CancellationSignal();
                Bitmap bitmapThumbnail = requireActivity().getContentResolver().loadThumbnail(uri, mSize, ca);
                System.out.println("THUMBNAIL: " + bitmapThumbnail);

                binding.debug.setImageBitmap(bitmapThumbnail);
            } catch (Exception e) {
                System.out.println("ERROR CREATING THUMBNAIL: " + e.getMessage());
//                if (Objects.requireNonNull(e.getMessage()).contains("audio")) {
//                    loadAudioFileThumbnail();
//                }
            }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
package it.startup.sendudes.ui.history;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Environment;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.io.File;
import java.util.Objects;

import it.startup.sendudes.R;
import it.startup.sendudes.databinding.FragmentHistoryBinding;
import it.startup.sendudes.utils.Db.FilesCursorAdapter;
import it.startup.sendudes.utils.Db.FilesDbAdapter;

public class HistoryFragment extends Fragment {

    private FragmentHistoryBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        FilesDbAdapter fda = new FilesDbAdapter(getContext()).open();

        Cursor cursor = fda.fetchAllFiles();
        FilesCursorAdapter cursorAdapter = new FilesCursorAdapter(binding.getRoot().getContext(), cursor, getActivity());
        ListView historyList = binding.historyList;
        historyList.setAdapter(cursorAdapter);
        fda.close();
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
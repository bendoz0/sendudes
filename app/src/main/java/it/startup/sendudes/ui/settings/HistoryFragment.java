package it.startup.sendudes.ui.settings;

import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

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

        final TextView textView = binding.textNotifications;
//        notificationsViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        Cursor cursor = fda.fetchAllFiles();
        String debug = "";
        if (cursor.moveToFirst()) {
            do {
                int columns = cursor.getColumnCount();
                for (int i = 1; i < columns; i++){
                    debug += cursor.getString(i) + "\n";
                }
            } while (cursor.moveToNext());
        }
        textView.setText(debug);
        fda.close();
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
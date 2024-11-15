package it.startup.sendudes.ui.receive;

import static it.startup.sendudes.utils.NetworkUtils.broadcast;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;


import it.startup.sendudes.databinding.FragmentReceiveBinding;

public class ReceiveFragment extends Fragment {

    private FragmentReceiveBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentReceiveBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        binding.btnSearchDevices.setOnClickListener(v -> onClickSearchDevices());

        return root;
    }

    private void onClickSearchDevices() {
        broadcast();
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
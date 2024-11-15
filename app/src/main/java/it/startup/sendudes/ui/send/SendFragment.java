package it.startup.sendudes.ui.send;

import static it.startup.sendudes.utils.NetworkUtils.getMyIP;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import it.startup.sendudes.databinding.FragmentSendBinding;

public class SendFragment extends Fragment {
    //FragmentHomeBinding is a class generated automatically when View Binding is enabled (in the android v8 and later on ig)
    private FragmentSendBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentSendBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        binding.btnGetIp.setOnClickListener(v -> onClickGetIp());
        return root;
    }
    private void onClickGetIp() {
//       got em 2 ways to access em ui elements usin java, the one right below this line basically uses a traditional way to access the ui elements, and this way doesn't provide type safety, on the other hand the viewModel way does cuz it's binded to the fragment.
//       Button btn = root.findViewById(R.id.clickmebtn);

        binding.textHome.setText(getMyIP());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
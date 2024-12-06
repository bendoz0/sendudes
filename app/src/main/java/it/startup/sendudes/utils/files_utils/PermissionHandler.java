package it.startup.sendudes.utils.files_utils;

import android.Manifest;
import android.os.Build;

import androidx.fragment.app.Fragment;

import com.permissionx.guolindev.PermissionX;

import java.util.ArrayList;
import java.util.List;

public class PermissionHandler {
    public static void askForFilePermission(Fragment fragment, PermissionGranted action) {
        List<String> permissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            permissions.add(Manifest.permission.MANAGE_EXTERNAL_STORAGE);
        } else {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        //Ask for file permissions
        PermissionX.init(fragment)
                .permissions(permissions)
                .onExplainRequestReason((scope, deniedList) -> scope.showRequestReasonDialog(deniedList, "To send and receive files we need PERMISSIOOOOONNSSSSSSSSSSS", "OK", "Cancel"))
                .request((allGranted, grantedList, deniedList) -> {
                    if (allGranted) {//Check granted privilages
                        action.onPermissionGranted();
                    }
                });
    }
}

package com.example.serialv2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.widget.Toast;

public class UsbPermissionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if ("com.example.serialv2.USB_PERMISSION".equals(action)) {
            UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                if (device != null) {
                    Toast.makeText(context, "USB permission granted", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, "USB permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

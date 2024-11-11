package com.example.serialv2;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.hoho.android.usbserial.driver.UsbSerialDriver;

import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder> {
    private final List<UsbSerialDriver> devices;
    private final OnDeviceClickListener listener;

    public DeviceAdapter(List<UsbSerialDriver> devices, OnDeviceClickListener listener) {
        this.devices = devices;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_item, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UsbSerialDriver device = devices.get(position);
        holder.deviceName.setText(String.format("Device: %s", device.getDevice().getDeviceName()));
        holder.deviceDetails.setText(String.format("Device ID : %d, Vendor ID : %d", device.getDevice().getDeviceId(), device.getDevice().getVendorId()));
        holder.itemView.setOnClickListener(v -> listener.onDeviceClick(device));
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private TextView deviceName;
        private TextView deviceDetails;

        ViewHolder(View itemView) {
            super(itemView);
            deviceName = itemView.findViewById(R.id.deviceName);
            deviceDetails = itemView.findViewById(R.id.deviceDetails);
        }
    }

    interface OnDeviceClickListener {
        void onDeviceClick(UsbSerialDriver device);
    }
}

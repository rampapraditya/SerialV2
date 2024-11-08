package com.example.serialv2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hoho.android.usbserial.driver.UsbSerialDriver;

import java.util.List;

public class DeviceAdapter2 extends RecyclerView.Adapter<DeviceAdapter2.ViewHolder> {
    private final List<UsbSerialDriver> devices;
    private final OnDeviceSelectedListener listener;

    public DeviceAdapter2(List<UsbSerialDriver> devices, OnDeviceSelectedListener listener) {
        this.devices = devices;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UsbSerialDriver device = devices.get(position);
        holder.deviceName.setText(String.format("Device: %s", device.getDevice().getDeviceName()));
        holder.itemView.setOnClickListener(v -> listener.onDeviceSelected(device));
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView deviceName;

        ViewHolder(View itemView) {
            super(itemView);
            deviceName = itemView.findViewById(R.id.deviceName);
        }
    }

    public interface OnDeviceSelectedListener {
        void onDeviceSelected(UsbSerialDriver driver);
    }
}

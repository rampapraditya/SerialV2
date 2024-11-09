package com.example.serialv2;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String ACTION_USB_PERMISSION = "com.example.serialv2.USB_PERMISSION";
    private UsbManager usbManager;
    private UsbSerialPort serialPort;
    private RecyclerView deviceRecyclerView;
    private EditText editTextSend;
    private TextView status, output;

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    @SuppressLint("UnsafeIntentLaunch") UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            openSerialPort(Objects.requireNonNull(getDriverForDevice(device)));
                        }
                    } else {
                        Toast.makeText(context, "Permission denied for device", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        deviceRecyclerView = findViewById(R.id.recyclerViewDevices);
        editTextSend = findViewById(R.id.txtData);
        status = findViewById(R.id.textViewStatus);
        output = findViewById(R.id.output);

        Button btnRefresh = findViewById(R.id.btnRefresh);
        Button buttonSend = findViewById(R.id.buttonSend);

        btnRefresh.setOnClickListener(v -> listDevices());
        buttonSend.setOnClickListener(v -> sendData());

        deviceRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        listDevices();

        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(usbReceiver, filter, RECEIVER_NOT_EXPORTED);
    }

    private void listDevices() {
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
        // Adapter untuk menampilkan daftar perangkat
        DeviceAdapter deviceAdapter = new DeviceAdapter(availableDrivers, this::requestPermission);
        deviceRecyclerView.setAdapter(deviceAdapter);
    }

    private void requestPermission(UsbSerialDriver driver) {
        UsbDevice device = driver.getDevice();
        if (!usbManager.hasPermission(device)) {
            PendingIntent permissionIntent = PendingIntent.getBroadcast(
                    this, 0,
                    new Intent(ACTION_USB_PERMISSION),
                    PendingIntent.FLAG_IMMUTABLE);
            usbManager.requestPermission(device, permissionIntent);
        } else {
            openSerialPort(driver);
        }
    }

    private void disconnectSerialPort() {
        try {
            if (serialPort != null) {
                if(serialPort.isOpen()){
                    serialPort.close();
                    updateConnectionStatus(false);
                    output.setText("Disconnected from current port");
                }
            }
        } catch (IOException e) {
            Toast.makeText(this, "Failed to close port", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateConnectionStatus(boolean isConnected) {
        if (isConnected) {
            status.setText("Status: Connected");
            status.setTextColor(Color.GREEN);
        } else {
            status.setText("Status: Disconnected");
            status.setTextColor(Color.RED);
        }
    }

    private UsbSerialDriver getDriverForDevice(UsbDevice device) {
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
        for (UsbSerialDriver driver : availableDrivers) {
            if (driver.getDevice().equals(device)) {
                return driver;
            }
        }
        return null;
    }

    private void openSerialPort(UsbSerialDriver driver) {
        output.setText("Opening serial port for driver: " + driver);
        if (!driver.getPorts().isEmpty()) {
            serialPort = driver.getPorts().get(0);

            try {
                assert serialPort != null;
                disconnectSerialPort();
                serialPort.open(usbManager.openDevice(driver.getDevice()));
                serialPort.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                startIoManager();
                Toast.makeText(this, "Port opened", Toast.LENGTH_SHORT).show();
                updateConnectionStatus(true);
            } catch (IOException e) {
                updateConnectionStatus(false);
                Toast.makeText(this, "Failed to open port", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startIoManager() {
        SerialInputOutputManager usbIoManager = new SerialInputOutputManager(serialPort, new SerialInputOutputManager.Listener() {
            @Override
            public void onNewData(byte[] data) {
                runOnUiThread(() -> {
                    String s = new String(data, StandardCharsets.UTF_8);
                    output.setText(s);
                });
            }

            @Override
            public void onRunError(Exception e) {
                runOnUiThread(() -> {
                    output.setText("Error : " + e.getMessage());
                });
            }
        });
        Executors.newSingleThreadExecutor().submit(usbIoManager);
    }

    public void sendData() {
        if (serialPort != null) {
            String data = editTextSend.getText().toString();
            try {
                serialPort.write(data.getBytes(), 1000);
                Toast.makeText(this, "Data sent", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Toast.makeText(this, "Failed to send data", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(usbReceiver);
        try {
            if (serialPort != null) {
                if(serialPort.isOpen()){
                    serialPort.close();
                    serialPort = null;
                }
            }
        } catch (IOException e) {
            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
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
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                listDevices();
                requestPermissionForAllDevices();
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                listDevices();
                closeSerialPort();
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

        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
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

    private void requestPermissionForAllDevices() {
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
        for (UsbSerialDriver driver : availableDrivers) {
            UsbDevice device = driver.getDevice();
            if (!usbManager.hasPermission(device)) {
                PendingIntent permissionIntent = PendingIntent.getBroadcast(
                        this, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);
                usbManager.requestPermission(device, permissionIntent);
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private void disconnectSerialPort() {
        try {
            if (serialPort != null) {
                if (serialPort.isOpen()) {
                    serialPort.close();
                    updateConnectionStatus(false);
                    output.setText("Disconnected from current port");
                }
            }
        } catch (IOException e) {
            Toast.makeText(this, "Failed to close port", Toast.LENGTH_SHORT).show();
        }
    }

    private void closeSerialPort() {
        if (serialPort != null) {
            try {
                if (serialPort.isOpen()) {
                    serialPort.close();
                    serialPort = null;
                    updateConnectionStatus(false);
                }
            } catch (IOException e) {
                Toast.makeText(this, "Error closing port", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateConnectionStatus(boolean isConnected) {
        if (isConnected) {
            status.setText("Status: Connected");
            status.setTextColor(Color.GREEN);
        } else {
            status.setText("Status: Disconnected");
            status.setTextColor(Color.RED);
        }
    }

    private void openSerialPort(UsbSerialDriver driver) {
        if (!driver.getPorts().isEmpty()) {
            serialPort = driver.getPorts().get(0);
            try {
                assert serialPort != null;
                disconnectSerialPort();
                serialPort.open(usbManager.openDevice(driver.getDevice()));
                serialPort.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                //startIoManager();
                startIoManagerV1();
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

            @SuppressLint("SetTextI18n")
            @Override
            public void onRunError(Exception e) {
                runOnUiThread(() -> output.setText("Error : " + e.getMessage()));
            }
        });
        Executors.newSingleThreadExecutor().submit(usbIoManager);
    }

    @SuppressLint("SetTextI18n")
    private void startIoManagerV1() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> {
            byte[] buffer = new byte[1024];  // buffer size may be adjusted as needed
            while (true) {
                try {
                    int len = serialPort.read(buffer, 100);  // `100` is a timeout in milliseconds
                    if (len > 0) {
                        String s = new String(buffer, 0, len, StandardCharsets.UTF_8);
                        runOnUiThread(() -> output.append(s));  // Append to show continuous data
                    }
                } catch (IOException e) {
                    runOnUiThread(() -> output.setText("Error: " + e.getMessage()));
                    break;  // Exit the loop on an error to prevent continuous retries
                }
            }
        });
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
        closeSerialPort();
    }
}
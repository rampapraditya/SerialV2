package com.example.serialv2;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
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
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String ACTION_USB_PERMISSION = "com.example.serialv2.USB_PERMISSION";
    private UsbManager usbManager;
    private UsbSerialPort serialPort;
    private RecyclerView deviceRecyclerView;
    private DeviceAdapter deviceAdapter; // Adapter untuk menampilkan daftar perangkat
    private EditText editTextSend;
    private Button btnRefresh, buttonSend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        deviceRecyclerView = findViewById(R.id.recyclerViewDevices);
        editTextSend = findViewById(R.id.txtData);
        btnRefresh = findViewById(R.id.btnRefresh);
        buttonSend = findViewById(R.id.buttonSend);

        btnRefresh.setOnClickListener(v -> listDevices());
        buttonSend.setOnClickListener(v -> sendData());

        deviceRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        listDevices();
    }

    private void listDevices() {
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
        if (availableDrivers.isEmpty()) {
            Toast.makeText(this, "No USB devices found", Toast.LENGTH_SHORT).show();
            return;
        }
        deviceAdapter = new DeviceAdapter(availableDrivers, this::requestPermission);
        deviceRecyclerView.setAdapter(deviceAdapter);
    }

    private void requestPermission(UsbSerialDriver driver) {
        UsbDevice device = driver.getDevice();
        PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);
        usbManager.requestPermission(device, permissionIntent);
        openSerialPort(driver);
    }

    private void openSerialPort(UsbSerialDriver driver) {
        if (!driver.getPorts().isEmpty()) {
            serialPort = driver.getPorts().get(0);
            try {
                serialPort.open(usbManager.openDevice(driver.getDevice()));
                serialPort.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                startIoManager();
                Toast.makeText(this, "Port opened", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
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
                    Toast.makeText(MainActivity.this, s, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onRunError(Exception e) {
                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
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
        try {
            if (serialPort != null) {
                serialPort.close();
            }
        } catch (IOException e) {
            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
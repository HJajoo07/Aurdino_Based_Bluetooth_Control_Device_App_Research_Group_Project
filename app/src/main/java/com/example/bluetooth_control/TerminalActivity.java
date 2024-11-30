package com.example.bluetooth_control;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

import okhttp3.*;

public class TerminalActivity extends AppCompatActivity {

    private static final String TAG = "TerminalActivity";
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 2;

    private BluetoothAdapter bluetoothAdapter;
    private ConnectThread connectThread;
    private ConnectedThread connectedThread;

    private TextView terminalText;
    private ScrollView scrollView;

    public class Constants {
        public static final int MESSAGE_READ = 1;
        // Add any other constants your application might need
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terminal);

        scrollView = findViewById(R.id.scrollView);
        terminalText = findViewById(R.id.terminalText);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Device doesn't support Bluetooth");
            finish();
            return;
        }
        // Check Bluetooth permissions
        checkBluetoothPermissions();

        // Retrieve the connected BluetoothSocket from the BluetoothHolder
        BluetoothSocketWrapper socketWrapper = BluetoothHolder.getBluetoothSocketWrapper();

        if (socketWrapper != null) {
            BluetoothSocket bluetoothSocket = socketWrapper.getBluetoothSocket();
            connectToDevice(bluetoothSocket);
        } else {
            Log.e(TAG, "BluetoothSocketWrapper is null");
            finish();
        }
    }

    // Method to send data to server
    private static void sendDataToServer(String data) {
        // Create an OkHttpClient instance
        OkHttpClient client = new OkHttpClient();

        // Create JSON data to send
        String jsonInputString = "{\"data\":\"" + data + "\"}";

        // Create a RequestBody with JSON data
        RequestBody body = RequestBody.create(
                jsonInputString,
                MediaType.get("application/json; charset=utf-8")
        );

        // Build the request
        Request request = new Request.Builder()
                .url("https://aurdino-control-backend.vercel.app/data") // Replace with the correct endpoint
                .post(body)
                .build();

        // Execute the request in a new thread
        new Thread(() -> {
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Data sent to server successfully");

                    // Optionally, read the response body
                    String responseBody = response.body().string();
                    Log.d(TAG, "Server response: " + responseBody);
                } else {
                    Log.e(TAG, "Failed to send data. Response Code: " + response.code());

                    // Optionally, read the error body
                    String errorBody = response.body().string();
                    Log.e(TAG, "Server error response: " + errorBody);
                }
            } catch (IOException e) {
                Log.e(TAG, "Exception while sending data to server", e);
            }
        }).start();
    }



    private void checkBluetoothPermissions() {
        String[] permissions = {
                android.Manifest.permission.BLUETOOTH,
                android.Manifest.permission.BLUETOOTH_ADMIN,
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.BLUETOOTH_SCAN
        };

        // Request Bluetooth permissions if not granted
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{permission}, REQUEST_BLUETOOTH_PERMISSIONS);
            }
        }
    }

    private void connectToDevice(BluetoothSocket bluetoothSocket) {
        connectThread = new ConnectThread(bluetoothSocket);
        connectThread.start();
    }

    // Handler to update the UI with received data
    private final Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_READ:
                    String data = (String) msg.obj;
                    updateTerminal(data);
                    return true;
            }
            return false;
        }
    });

    // Method to update the TextView in the UI thread
    private void updateTerminal(String data) {
        runOnUiThread(() -> {
            terminalText.append(data + "\n");

            // Scroll to the bottom of the ScrollView
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        });
    }

    // Thread to manage Bluetooth connection

    // Inside TerminalActivity class
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;

        public ConnectThread(BluetoothSocket socket) {
            mmSocket = socket;
        }

        public void run() {
            // Check for BLUETOOTH_ADMIN permission before canceling discovery
            if (hasBluetoothPermission(android.Manifest.permission.BLUETOOTH_ADMIN)) {
                try {
                    if (!mmSocket.isConnected()) {
                        mmSocket.connect(); // Connect to the device
                    }

                    // Connected successfully, start the ConnectedThread
                    connectedThread = new ConnectedThread(mmSocket, handler);
                    connectedThread.start();
                } catch (IOException connectException) {
                    Log.e(TAG, "Error occurred during socket connection", connectException);
                    cancel();
                } catch (SecurityException se) {
                    Log.e(TAG, "SecurityException: Permission not granted for socket connection", se);
                    cancel();
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "IllegalArgumentException: Invalid UUID or socket parameter", e);
                    cancel();
                } catch (Exception e) {
                    Log.e(TAG, "Exception during socket connection", e);
                    cancel();
                }
            } else {
                Log.e(TAG, "BLUETOOTH_ADMIN permission not granted");
                // Handle accordingly, e.g., show a message to the user
                cancel();
            }
        }

        public void cancel() {
            try {
                if (mmSocket != null && mmSocket.isConnected()) {
                    mmSocket.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }

        // Helper method to check if Bluetooth permission is granted
        private boolean hasBluetoothPermission(String permission) {
            return ContextCompat.checkSelfPermission(TerminalActivity.this, permission) == PackageManager.PERMISSION_GRANTED;
        }
    }

    // Thread to manage Bluetooth connection
    private static class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final Handler handler;

        public ConnectedThread(BluetoothSocket socket, Handler handler) {
            mmSocket = socket;
            this.handler = handler;
            InputStream tmpIn = null;

            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input stream", e);
            }

            mmInStream = tmpIn;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int numBytes;

            while (true) {
                try {
                    numBytes = mmInStream.read(buffer);
                    String data = new String(buffer, 0, numBytes);
                    handler.obtainMessage(Constants.MESSAGE_READ, numBytes, -1, data).sendToTarget();
                    sendDataToServer(data);
                } catch (IOException e) {
                    Log.d(TAG, "Input stream was disconnected", e);
                    break;
                }
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }
}



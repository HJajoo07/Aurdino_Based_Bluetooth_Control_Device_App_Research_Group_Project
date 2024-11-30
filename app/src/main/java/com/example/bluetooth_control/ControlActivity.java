package com.example.bluetooth_control;

import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.OutputStream;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ControlActivity extends AppCompatActivity {

    private Button onButton, offButton;
    private TextView onTextView, offTextView;

    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;

    private static final char ON_COMMAND = 'A';
    private static final char OFF_COMMAND = 'B';
    private static final String TAG = "ControlActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);

        // Initialize UI elements
        onButton = findViewById(R.id.onButton);
        offButton = findViewById(R.id.offButton);
        onTextView = findViewById(R.id.onTextView);
        offTextView = findViewById(R.id.offTextView);

        // Initialize Bluetooth connection
        initializeBluetoothConnection();

        // Set button listeners
        onButton.setOnClickListener(view -> sendCommand(ON_COMMAND));
        offButton.setOnClickListener(view -> sendCommand(OFF_COMMAND));
    }

    private void initializeBluetoothConnection() {
        BluetoothSocketWrapper socketWrapper = BluetoothHolder.getBluetoothSocketWrapper();

        if (socketWrapper != null) {
            bluetoothSocket = socketWrapper.getBluetoothSocket();

            if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
                try {
                    outputStream = bluetoothSocket.getOutputStream();
                } catch (IOException e) {
                    showToast("Failed to obtain OutputStream");
                    e.printStackTrace();
                }
            } else {
                showToast("Bluetooth socket is not connected");
            }
        } else {
            showToast("BluetoothSocketWrapper is null");
        }
    }

    private void sendCommand(char command) {
        if (outputStream == null) {
            showToast("OutputStream is null. Can't send command.");
            return;
        }

        try {
            // Write the command to the output stream
            outputStream.write(command);
            updateStatus(command);
        } catch (IOException e) {
            showToast("Error sending command");
            e.printStackTrace();
        }
    }

    private void updateStatus(char command) {
        String status = "";
        if (command == ON_COMMAND) {
            onTextView.setText("Status: Device is ON");
            offTextView.setText(""); // Clear offTextView
            status = "ON";
        } else if (command == OFF_COMMAND) {
            offTextView.setText("Status: Device is OFF");
            onTextView.setText("");  // Clear onTextView
            status = "OFF";
        }

        // Send status to the server
        sendDataToServer(status);
    }

    private void sendDataToServer(String data) {
        // Use OkHttpClient to send the status to the backend API
        OkHttpClient client = new OkHttpClient();

        String jsonInputString = "{\"data\":\"" + data + "\"}";

        RequestBody body = RequestBody.create(
                jsonInputString,
                MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url("https://aurdino-control-backend.vercel.app/data") // Replace with the correct API endpoint
                .post(body)
                .build();

        new Thread(() -> {
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    // Optionally, log the response from the server
                    String responseBody = response.body().string();
                    runOnUiThread(() -> showToast("Status sent to server successfully"));
                } else {
                    // Handle the error response
                    runOnUiThread(() -> showToast("Failed to send status to server"));
                }
            } catch (IOException e) {
                runOnUiThread(() -> showToast("Error sending status to server"));
                e.printStackTrace();
            }
        }).start();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeBluetoothSocket();
    }

    private void closeBluetoothSocket() {
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                showToast("Error closing Bluetooth socket");
                e.printStackTrace();
            }
        }
    }
}


package com.example.bluetooth_control;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice connectedDevice;
    private BluetoothSocket bluetoothSocket;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 2;
    private static final String HC_05_DEVICE_NAME = "HC-05";
    private static final UUID HC_05_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            Toast.makeText(this, "Device doesn't support Bluetooth", Toast.LENGTH_SHORT).show();
            finish(); // Close the app
        }
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(discoveryReceiver, filter);
    }

    private void connectToDevice(BluetoothDevice device) {
        try {
            // Check if Bluetooth permissions are granted
            if (checkBluetoothPermissions()) {
                // Create a BluetoothSocket using the HC-05 UUID
                BluetoothSocket socket = device.createRfcommSocketToServiceRecord(HC_05_UUID);

                // Cancel discovery before connecting
                bluetoothAdapter.cancelDiscovery();

                // Connect to the device
                socket.connect();

                // Set the connected device and socket
                setConnectedDevice(device, socket);
                BluetoothHolder.setBluetoothSocketWrapper(new BluetoothSocketWrapper(socket));

                Toast.makeText(this, "Connected to HC-05", Toast.LENGTH_SHORT).show();
            } else {
                // Bluetooth permissions not granted, handle accordingly (e.g., show a message to the user)
                Toast.makeText(this, "Bluetooth permissions not granted, cannot connect to HC-05", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            // Handle the IOException
            Toast.makeText(this, "Failed to connect to HC-05", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        } catch (SecurityException se) {
            // Handle the SecurityException
            Toast.makeText(this, "SecurityException: Permission not granted", Toast.LENGTH_SHORT).show();
            se.printStackTrace();
        }
    }

    public void openBluetoothActivity(View view) {
        try {
            if (bluetoothAdapter != null && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED) {
                // Bluetooth permission is granted
                if (!bluetoothAdapter.isEnabled()) {
                    // Bluetooth is not enabled, ask the user to enable it
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                } else {
                    // Bluetooth is already enabled, start paired devices activity
                    openPairedDevicesActivity();
                }
            } else {
                // Bluetooth permission is not granted or BluetoothAdapter is null
                Toast.makeText(this, "Bluetooth permission not granted or Bluetooth not supported", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error opening Bluetooth activity", Toast.LENGTH_SHORT).show();
        }
    }
    private BluetoothDevice getHC05Device() {
        // Check if Bluetooth permissions are granted
        if (checkBluetoothPermissions()) {
            Set<BluetoothDevice> pairedDevices = null;
            try {
                // Try to get paired devices
                pairedDevices = bluetoothAdapter.getBondedDevices();
            } catch (SecurityException e) {
                // Handle SecurityException
                Toast.makeText(this, "SecurityException: Unable to get paired devices", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
                return null;
            }

            if (pairedDevices != null) {
                for (BluetoothDevice device : pairedDevices) {
                    // Check if device name is not null and equals HC-05 device name
                    if (device != null) {
                        // Check BLUETOOTH_CONNECT permission before accessing device name
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                            if (HC_05_DEVICE_NAME.equals(device.getName())) {
                                return device;
                            }
                        } else {
                            // Handle case where permission is not granted
                            Toast.makeText(this, "Bluetooth permission not granted, device name not accessible", Toast.LENGTH_SHORT).show();
                            return null;
                        }
                    }
                }
            }
        } else {
            // Bluetooth permissions not granted, handle accordingly (e.g., show a message to the user)
            Toast.makeText(this, "Bluetooth permissions not granted, cannot proceed", Toast.LENGTH_SHORT).show();
        }
        return null;
    }


    // Helper method to check if Bluetooth permission is granted



    private void openPairedDevicesActivity() {
        // Check if Bluetooth permissions are granted
        if (checkBluetoothPermissions()) {
            // Get the HC-05 device
            BluetoothDevice hc05Device = getHC05Device();

            // If HC-05 device is found, connect automatically
            if (hc05Device != null) {
                connectToDevice(hc05Device);
            } else {
                // Get the list of paired devices
                Set<BluetoothDevice> pairedDevices;
                try {
                    // Try to get paired devices
                    pairedDevices = bluetoothAdapter.getBondedDevices();
                } catch (SecurityException e) {
                    // Handle SecurityException
                    Toast.makeText(this, "SecurityException: Unable to get paired devices", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                    return;
                }

                if (pairedDevices.size() > 0) {
                    // Convert Set to an array
                    BluetoothDevice[] devicesArray = pairedDevices.toArray(new BluetoothDevice[0]);

                    // Create a list of device names for the AlertDialog
                    String[] deviceNames = new String[devicesArray.length];
                    for (int i = 0; i < devicesArray.length; i++) {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                            deviceNames[i] = devicesArray[i].getName();
                        } else {
                            // Handle case where permission is not granted
                            deviceNames[i] = "Unknown Device";
                            Toast.makeText(this, "Bluetooth permission not granted, device name not accessible", Toast.LENGTH_SHORT).show();
                        }
                    }

                    // Create an AlertDialog to display the list of paired devices
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Select a device")
                            .setItems(deviceNames, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // Connect to the selected device
                                    connectToDevice(devicesArray[which]);
                                }
                            });

                    builder.show();
                } else {
                    Toast.makeText(this, "No paired devices found", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            // Bluetooth permissions not granted, handle accordingly (e.g., show a message to the user)
            Toast.makeText(this, "Bluetooth permissions not granted, cannot proceed", Toast.LENGTH_SHORT).show();
        }
    }



    private boolean checkBluetoothPermissions() {
        // List of required Bluetooth permissions
        String[] bluetoothPermissions = {
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
        };

        // Check if the app has Bluetooth permissions
        boolean permissionsGranted = true;
        for (String permission : bluetoothPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsGranted = false;
                break;
            }
        }

        if (!permissionsGranted) {
            // Request Bluetooth permissions
            ActivityCompat.requestPermissions(this, bluetoothPermissions, REQUEST_BLUETOOTH_PERMISSIONS);
        }

        return permissionsGranted;
    }

    @SuppressWarnings("MissingPermission")
    private void startDiscovery() {
        // Check if Bluetooth permissions are granted
        if (checkBluetoothPermissions()) {
            // Bluetooth permissions are granted
            // Check if Bluetooth is enabled
            if (!bluetoothAdapter.isEnabled()) {
                Toast.makeText(this, "Bluetooth is not enabled", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if Bluetooth discovery is already in progress
            if (bluetoothAdapter.isDiscovering()) {
                try {
                    bluetoothAdapter.cancelDiscovery();
                } catch (SecurityException se) {
                    // Handle SecurityException
                    Toast.makeText(this, "SecurityException: Unable to cancel discovery", Toast.LENGTH_SHORT).show();
                    se.printStackTrace();
                }
            }

            // Check for BLUETOOTH_SCAN permission explicitly before starting Bluetooth discovery
            if (hasBluetoothPermission(Manifest.permission.BLUETOOTH_SCAN)) {
                // Start discovery
                bluetoothAdapter.startDiscovery();
            } else {
                Toast.makeText(this, "BLUETOOTH_SCAN permission not granted, cannot proceed with discovery", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Bluetooth permissions not granted, cannot proceed", Toast.LENGTH_SHORT).show();
        }
    }

    // Helper method to check if Bluetooth permission is granted
    private boolean hasBluetoothPermission(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    private final BroadcastReceiver discoveryReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            try {
                String action = intent.getAction();

                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    // Check if Bluetooth permissions are granted
                    if (checkBluetoothPermissions()) {
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                        if (device != null) {
                            // Check if the discovered device is the HC-05 device
                            if (HC_05_DEVICE_NAME.equals(device.getName())) {
                                connectToDevice(device);
                            }
                        }
                    } else {
                        // Bluetooth permissions not granted, handle accordingly (e.g., show a message to the user)
                        Toast.makeText(context, "Bluetooth permissions not granted, cannot proceed", Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (SecurityException se) {
                // Handle the SecurityException
                Toast.makeText(context, "SecurityException: Permission not granted", Toast.LENGTH_SHORT).show();
                se.printStackTrace();
            }
        }
    };


    // Handle the result of the Bluetooth enable request
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                // Bluetooth is enabled, check Bluetooth permissions
                checkBluetoothPermissions();
            } else {
                Toast.makeText(this, "Bluetooth not enabled, cannot proceed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Handle the result of the Bluetooth permissions request
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            // Check if the permissions are granted
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                // Permissions are granted, start Bluetooth discovery or perform other actions
                startDiscovery();
            } else {
                Toast.makeText(this, "Bluetooth permissions not granted, cannot proceed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setConnectedDevice(BluetoothDevice device, BluetoothSocket socket) {
        connectedDevice = device;
        bluetoothSocket = socket;
        BluetoothHolder.setBluetoothSocketWrapper(new BluetoothSocketWrapper(socket));
    }

    public void openControlActivity(View view) {
        // Check if Bluetooth is enabled
        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Bluetooth is not enabled", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if a device is connected
        if (bluetoothSocket == null) {
            Toast.makeText(this, "No connected device", Toast.LENGTH_SHORT).show();
            return;
        }

        // Start ControlActivity with BluetoothSocket
        Intent intent = new Intent(this, ControlActivity.class);
        intent.putExtra("bluetoothSocketWrapper", new BluetoothSocketWrapper(bluetoothSocket));
        startActivity(intent);
    }

    public void openTerminalActivity(View view) {
        if (connectedDevice != null) {
            Intent intent = new Intent(this, TerminalActivity.class);
            intent.putExtra("bluetoothDevice", connectedDevice);
            startActivity(intent);
        } else {
            Toast.makeText(this, "No connected device", Toast.LENGTH_SHORT).show();
        }
    }

}
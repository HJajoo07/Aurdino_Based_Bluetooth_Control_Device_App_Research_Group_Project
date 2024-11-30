package com.example.bluetooth_control;

public class BluetoothHolder {
    private static BluetoothSocketWrapper bluetoothSocketWrapper;

    public static BluetoothSocketWrapper getBluetoothSocketWrapper() {
        return bluetoothSocketWrapper;
    }

    public static void setBluetoothSocketWrapper(BluetoothSocketWrapper wrapper) {
        bluetoothSocketWrapper = wrapper;
    }
}

package com.example.bluetooth_control;

import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class BluetoothSocketWrapper implements Serializable {
    private transient BluetoothSocket bluetoothSocket;

    public BluetoothSocketWrapper(BluetoothSocket socket) {
        this.bluetoothSocket = socket;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }

    public BluetoothSocket getBluetoothSocket() {
        return bluetoothSocket;
    }
}

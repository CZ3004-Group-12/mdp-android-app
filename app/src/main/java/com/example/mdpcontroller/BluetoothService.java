package com.example.mdpcontroller;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class BluetoothService {
    public static BluetoothAdapter mBluetoothAdapter;
    public static BluetoothSocket mBluetoothSocket;
    public ConnectedThread mConnectedThread;
    public Activity mContext;
    public  enum BluetoothStatus {
        UNCONNECTED, SCANNING, CONNECTED, DISCONNECTED
    }
    private static BluetoothStatus btStatus;
    private static  final String[] permissions = {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    // sends bt_status_changed broadcast when status is set
    public static void setBtStatus(BluetoothStatus newStatus, Map<String, String> extras, Activity context) {
        btStatus = newStatus;
        Intent intent = new Intent("bt_status_changed");
        for (String key: extras.keySet()){
            intent.putExtra(key, extras.get(key));
        }
        context.sendBroadcast(intent);
    }

    public static BluetoothStatus getBtStatus(){
        return btStatus;
    }

    public static void initialize(Activity activity){
        btStatus = BluetoothStatus.UNCONNECTED;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!hasPermissions(activity)) {
            ActivityCompat.requestPermissions(activity, permissions, 1);
        }
    }

    private static boolean hasPermissions(Activity activity) {
        if (permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void stopSearch() {
        mBluetoothAdapter.cancelDiscovery();
        btStatus = BluetoothStatus.UNCONNECTED;
    }

    public static void startSearch() {
        mBluetoothAdapter.startDiscovery();
        btStatus = BluetoothStatus.SCANNING;
    }

    public BluetoothService(Activity context){
        mContext = context;
    }

    public void startConnectedThread() {
        mConnectedThread = new ConnectedThread(mBluetoothSocket);
        mConnectedThread.start();
    }

    public void write(String message){
        mConnectedThread.write(message.getBytes());
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                System.out.println("Error occurred when creating input stream " + e.getMessage());
            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                System.out.println("Error occurred when creating output stream " + e.getMessage());
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            // mmBuffer store for the stream
            byte[] buffer = new byte[1024];
            int numBytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                try {
                    // Read from the InputStream.
                    numBytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI activity.
                    Intent intent = new Intent("message_received");
                    intent.putExtra("message", new String(buffer, 0, numBytes));
                    mContext.sendBroadcast(intent);
                } catch (IOException e) {
                    System.out.println("Input stream was disconnected " + e.getMessage());
                    break;
                }
            }
        }

        // Call this from the main activity to send data to the remote device.
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                System.out.println("Error occurred when sending data " + e.getMessage());

                // Send a failure message back to the activity.
                Intent intent = new Intent("message_received");
                intent.putExtra("message", "Couldn't send data to the other device");
                mContext.sendBroadcast(intent);
            }
        }

        // Call this method from the main activity to shut down the connection.
        public void cancel() {
            try {
                mmSocket.close();
                setBtStatus(BluetoothStatus.UNCONNECTED, new HashMap<>(), mContext);
            } catch (IOException e) {
                System.out.println("Could not close the connect socket " + e.getMessage());
            }
        }
    }

}

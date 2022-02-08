package com.example.mdpcontroller;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.provider.Settings;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class BluetoothService {
    public static BluetoothAdapter mBluetoothAdapter;
    public static BluetoothSocket mBluetoothSocket;
    public static BluetoothDevice mConnectedDevice;
    public static final boolean RECONNECT_AS_CLIENT = false;
    public enum BluetoothStatus {
        UNCONNECTED, SCANNING, CONNECTING, CONNECTED, DISCONNECTED
    }

    private static BluetoothStatus btStatus;
    private static  final String[] permissions = {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };
    private static final UUID BT_MODULE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier
    private static final int MAX_RECONNECT_RETRIES = 5;
    private static final String NAME = "MDP_Group_12_Tablet";
    public ConnectedThread mConnectedThread;
    public AcceptThread mAcceptThread;
    public ConnectAsClientThread mClientThread;
    public Activity mContext;



    // sends bt_status_changed broadcast when status is set
    public static void setBtStatus(BluetoothStatus newStatus, Map<String, String> extras, Activity context) {
        btStatus = newStatus;
        Intent intent = new Intent("bt_status_changed");
        for (String key: extras.keySet()){
            intent.putExtra(key, extras.get(key));
        }
        System.out.println("BtStatus changed to "+newStatus.toString());
        context.sendBroadcast(intent);
    }

    public static BluetoothStatus getBtStatus(){
        return btStatus;
    }

    public static void initialize(Activity activity){
        setBtStatus(BluetoothStatus.UNCONNECTED, new HashMap<String, String>(), activity);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Request permissions
        if (!hasPermissions(activity)) {
            ActivityCompat.requestPermissions(activity, permissions, 1);
        }

        // Request to turn on bluetooth
        if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivity(enableBtIntent);
        }

        // Request to turn on location
        LocationManager lm = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                !lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            // Build the alert dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle("Location Services Not Active");
            builder.setMessage("Please enable Location Services and GPS");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    // Show location settings when the user acknowledges the alert dialog
                    Intent enableLocIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    activity.startActivity(enableLocIntent);
                }
            });
            Dialog alertDialog = builder.create();
            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.show();
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
    }

    public static void startSearch() {
        mBluetoothAdapter.startDiscovery();
    }

    public BluetoothService(Activity context){
        mContext = context;
    }

    public void startConnectedThread() {
        mConnectedThread = new ConnectedThread(mBluetoothSocket);
        mConnectedThread.start();
    }

    public boolean write(String message){
        if (mConnectedThread != null){
            mConnectedThread.write(message.getBytes());
            return true;
        }
        Toast.makeText(mContext, "Bluetooth not connected!", Toast.LENGTH_SHORT).show();
        return false;
    }

    public void disconnect() {
        if (mConnectedThread != null) mConnectedThread.cancel();
        mConnectedDevice = null;
    }

    public void clientConnect(String address, String name, Activity context) {
        mClientThread = new ConnectAsClientThread(address, name, context);
        mClientThread.start();
    }

    public void serverStartListen(Activity context) {
        if(mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE)
        {
            // make device discoverable
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            context.startActivityForResult(discoverableIntent, 1);
        }
        if (mAcceptThread == null){
            mAcceptThread = new AcceptThread(context);
            mAcceptThread.start();
        }
    }

    public void serverStopListen() {
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }
    }

    private static BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        try {
            return device.createInsecureRfcommSocketToServiceRecord(BT_MODULE_UUID);
        } catch (Exception e) {
            System.out.println("Could not create connection");
        }
        return  device.createRfcommSocketToServiceRecord(BT_MODULE_UUID);
    }

    private static class ConnectAsClientThread extends Thread {
        public Activity context;
        public String name, address;

        public ConnectAsClientThread(String address, String name, Activity context) {
            this.context = context;
            this.address = address;
            this.name = name;
        }

        @Override
        public void run() {
            System.out.println("starts");
            boolean fail = false;
            BluetoothService.stopSearch();
            BluetoothDevice device = BluetoothService.mBluetoothAdapter.getRemoteDevice(address);

            try {
                BluetoothService.mBluetoothSocket = createBluetoothSocket(device);
            } catch (IOException e) {
                fail = true;
                Intent intent = new Intent("message_received");
                intent.putExtra("message", "Socket creation failed");
                context.sendBroadcast(intent);
            }
            // Establish the Bluetooth socket connection.
            try {
                BluetoothService.mBluetoothSocket.connect();
                BluetoothService.mConnectedDevice = device;
            } catch (IOException e) {
                try {
                    fail = true;
                    System.out.println(e.getMessage());
                    BluetoothService.mBluetoothSocket.close();
                } catch (IOException e2) {
                    Intent intent = new Intent("message_received");
                    intent.putExtra("message", "Socket creation failed");
                    context.sendBroadcast(intent);
                }
            }
            System.out.println("ends");
            if(!fail) {
                Intent intent = new Intent("message_received");
                intent.putExtra("message", "Connected!");
                context.sendBroadcast(intent);
                Map<String, String> extra = new HashMap<>();
                extra.put("device", !name.equals("null") ? name : address);
                BluetoothService.setBtStatus(BluetoothService.BluetoothStatus.CONNECTED, extra, context);
            } else {
                Intent intent = new Intent("message_received");
                intent.putExtra("message", "Connection Failed");
                context.sendBroadcast(intent);
                Map<String, String> extra = new HashMap<>();
                BluetoothService.setBtStatus(BluetoothStatus.UNCONNECTED, extra, context);
            }
        }
    }

    private static class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;
        private final Activity context;

        public AcceptThread(Activity context) {
            this.context = context;
            BluetoothServerSocket tmp = null;
            try {
                tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME, BT_MODULE_UUID);
            } catch (IOException e) {
                System.out.println("Socket's listen() method failed");
            }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned.
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    System.out.println("Socket's accept() method failed");
                    break;
                }

                if (socket != null) {
                    // A connection was accepted. Perform work associated with
                    // the connection in a separate thread.
                    mBluetoothSocket = socket;
                    mConnectedDevice = socket.getRemoteDevice();
                    String name = socket.getRemoteDevice().getName();
                    String address = socket.getRemoteDevice().getAddress();
                    Map<String, String> extra = new HashMap<>();
                    extra.put("device", !name.equals("null") ? name : address);
                    Intent intent = new Intent("message_received");
                    intent.putExtra("message", "Connected!");
                    context.sendBroadcast(intent);
                    BluetoothService.setBtStatus(BluetoothService.BluetoothStatus.CONNECTED, extra, context);
                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                        System.out.println(e.getMessage());;
                    }
                    break;
                }
            }
        }

        // Closes the connect socket and causes the thread to finish.
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                System.out.println("Could not close the connect socket");
            }
        }
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
                    setBtStatus(BluetoothStatus.DISCONNECTED, new HashMap<>(), mContext);
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
            } catch (IOException e) {
                System.out.println("Could not close the connect socket " + e.getMessage());
            }
        }
    }

    // Receiver for DEVICE_DISCONNECTED, automatically tries to reconnect as client
    public class BluetoothLostReceiver extends BroadcastReceiver {

        Activity main;

        public BluetoothLostReceiver(Activity main) {
            super();
            this.main = main;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (getBtStatus() == BluetoothStatus.DISCONNECTED) {
                Intent intent2 = new Intent("message_received");
                if (RECONNECT_AS_CLIENT){
                    intent2.putExtra("message", "Connection lost, attempting to reconnect...");
                    context.sendBroadcast(intent2);
                    if(getBtStatus() == BluetoothStatus.DISCONNECTED && mConnectedDevice != null) {
                        for (int i = 0; i < MAX_RECONNECT_RETRIES; i++) {
                            if (btStatus == BluetoothStatus.CONNECTED) return;
                            try {
                                clientConnect(mConnectedDevice.getAddress(), mConnectedDevice.getName(), main);
                            } catch (Exception e) {
                                System.out.println(e.getMessage());
                            }
                            try {
                                intent2 = new Intent("message_received");
                                intent2.putExtra("message", "Reconnect attempt " + i + " failed");
                                context.sendBroadcast(intent2);
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                System.out.println(e.getMessage());
                            }
                        }
                    }
                    // Max tries elapsed
                    intent2 = new Intent("message_received");
                    intent2.putExtra("message", "Reconnect failed");
                    context.sendBroadcast(intent2);
                    setBtStatus(BluetoothStatus.UNCONNECTED, new HashMap<>(), (Activity) context);
                }
                else {
                    // reconnect as server
                    intent2.putExtra("message", "Connection lost, making device discoverable...");
                    context.sendBroadcast(intent2);
                    serverStartListen(main);
                }
            }
        }
    }

}

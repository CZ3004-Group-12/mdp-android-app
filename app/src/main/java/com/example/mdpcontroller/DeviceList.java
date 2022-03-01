package com.example.mdpcontroller;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class DeviceList extends AppCompatActivity {
    public List<String> deviceList;
    public RecyclerView rv;
    private DeviceListAdapter deviceListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);
        initializeDeviceList();
        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(DeviceFoundReceiver, filter);
        // Register for bt_status_changed broadcast, close device list if client connects
        filter = new IntentFilter("bt_status_changed");
        registerReceiver(BtStatusReceiver, filter);
    }

    private void initializeDeviceList(){
        // setup recyclerview
        rv = findViewById(R.id.rv);
        deviceList = new ArrayList<>();
        deviceListAdapter = new DeviceListAdapter(this);
        rv.setAdapter(deviceListAdapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rv.setLayoutManager(layoutManager);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(rv.getContext(),
                layoutManager.getOrientation());
        rv.addItemDecoration(dividerItemDecoration);

        if (BluetoothService.getBtStatus() == BluetoothService.BluetoothStatus.CONNECTED){
            // Device connected, display device and disconnect button
            String name = BluetoothService.mConnectedDevice.getName() + "\n" + BluetoothService.mConnectedDevice.getAddress();
            deviceList.clear();
            deviceList.add(name);
            deviceListAdapter.notifyItemInserted(deviceList.size());
            deviceListAdapter.setConnectedDeviceStr(true);
            Button bt = findViewById(R.id.button);
            bt.setText(R.string.disconnect_device);
        } else {
            // No devices connected, search for devices
            BluetoothService.startSearch();
            BluetoothService.setBtStatus(BluetoothService.BluetoothStatus.SCANNING, new HashMap<>(), this);
        }
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver DeviceFoundReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String name;
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // device found
                // get BluetoothDevice from the intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getName() == null) return;
                name = device.getName() + "\n" + device.getAddress();
                if (!deviceList.contains(name)){
                    deviceList.add(name);
                    deviceListAdapter.notifyItemInserted(deviceList.size());
                }
            }
        }
    };

    // Create a BroadcastReceiver for bt_status_changed
    private final BroadcastReceiver BtStatusReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (BluetoothService.getBtStatus() == BluetoothService.BluetoothStatus.CONNECTED) {
                finish();
            }
        }
    };

    public void toggleScan(View view){
        System.out.println("toggleScan");
        int resourceId = 0;
        // Stop Scan
        if (BluetoothService.getBtStatus() == BluetoothService.BluetoothStatus.SCANNING){
            BluetoothService.stopSearch();
            BluetoothService.setBtStatus(BluetoothService.BluetoothStatus.UNCONNECTED, new HashMap<String, String>(),this);
            resourceId = this.getResources().getIdentifier("@string/start_scan", "string", this.getPackageName());
        }
        // Start Scan
        else if (BluetoothService.getBtStatus() == BluetoothService.BluetoothStatus.UNCONNECTED ||
                BluetoothService.getBtStatus() == BluetoothService.BluetoothStatus.DISCONNECTED ||
                BluetoothService.getBtStatus() == BluetoothService.BluetoothStatus.CONNECTING){
            deviceList.clear();
            deviceListAdapter.notifyDataSetChanged();
            BluetoothService.startSearch();
            BluetoothService.setBtStatus(BluetoothService.BluetoothStatus.SCANNING, new HashMap<String, String>(),this);
            resourceId = this.getResources().getIdentifier("@string/stop_scan", "string", this.getPackageName());
            ((TextView)view).setText(resourceId);
        }
        // Disconnect Device
        else if (BluetoothService.getBtStatus() == BluetoothService.BluetoothStatus.CONNECTED){
            TextView tv = findViewById(R.id.textViewDeviceConnected);
            tv.setText("");
            deviceList.clear();
            deviceListAdapter.notifyDataSetChanged();
            resourceId = this.getResources().getIdentifier("@string/start_scan", "string", this.getPackageName());
            BluetoothService.setBtStatus(BluetoothService.BluetoothStatus.UNCONNECTED, new HashMap<String, String>(),this);
            deviceListAdapter.setConnectedDeviceStr(false);
        }
        ((TextView)view).setText(resourceId);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            // Unregister listener
            unregisterReceiver(DeviceFoundReceiver);
            unregisterReceiver(BtStatusReceiver);
        } catch (Exception e) {
            // already unregistered
        }
    }
}
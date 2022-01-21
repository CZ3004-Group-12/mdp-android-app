package com.example.mdpcontroller;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
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
    }

    private void initializeDeviceList(){
        // Lookup the recyclerview in activity layout
        rv = findViewById(R.id.rv);
        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);
        BluetoothService.startSearch();
//        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
//
//        List<String> s = new ArrayList<>();
//        for(BluetoothDevice bt : pairedDevices)
//            s.add(bt.getName());
        deviceList = new ArrayList<>();
        deviceListAdapter = new DeviceListAdapter(this);
        rv.setAdapter(deviceListAdapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rv.setLayoutManager(layoutManager);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(rv.getContext(),
                layoutManager.getOrientation());
        rv.addItemDecoration(dividerItemDecoration);
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String name;
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                name = device.getName() + "\n" + device.getAddress();
                System.out.println(name);
                if (!deviceList.contains(name)){
                    deviceList.add(name);
                    deviceListAdapter.notifyItemInserted(deviceList.size());
                }
                // String deviceHardwareAddress = device.getAddress(); // MAC address

            }
        }
    };

    public void toggleScan(View view){
        if (BluetoothService.getBtStatus() == BluetoothService.BluetoothStatus.SCANNING){
            BluetoothService.stopSearch();
            int resourceId = this.getResources().getIdentifier("@string/start_scan", "string", this.getPackageName());
            ((TextView)view).setText(resourceId);
        }
        else if (BluetoothService.getBtStatus() == BluetoothService.BluetoothStatus.UNCONNECTED){
            deviceList.clear();
            deviceListAdapter.notifyDataSetChanged();
            BluetoothService.startSearch();
            int resourceId = this.getResources().getIdentifier("@string/stop_scan", "string", this.getPackageName());
            ((TextView)view).setText(resourceId);
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(receiver);
    }
}
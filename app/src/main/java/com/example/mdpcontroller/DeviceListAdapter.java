package com.example.mdpcontroller;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.ViewHolder> {
    private List<String> localDataSet;
    private final View.OnClickListener mOnClickListener = new BluetoothOnClickListener();
    private final DeviceList parent;
    private static final UUID BT_MODULE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier

    private class BluetoothOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if(!BluetoothService.mBluetoothAdapter.isEnabled()) {
                Toast.makeText(parent, "Bluetooth not on", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(parent, "Connecting...", Toast.LENGTH_SHORT).show();
            // Get the device MAC address, which is the last 17 chars in the View
            int itemPosition = parent.rv.getChildLayoutPosition(v);
            String info = parent.deviceList.get(itemPosition);
            final String address = info.substring(info.length() - 17);
            final String name = info.substring(0,info.length() - 18);
            System.out.println(address);

            // Spawn a new thread to avoid blocking the GUI one
            new Thread()
            {
                @Override
                public void run() {
                    boolean fail = false;
                    BluetoothService.stopSearch();
                    BluetoothDevice device = BluetoothService.mBluetoothAdapter.getRemoteDevice(address);

                    try {
                        BluetoothService.mBluetoothSocket = createBluetoothSocket(device);
                    } catch (IOException e) {
                        fail = true;
                        Toast.makeText(parent, "Socket creation failed", Toast.LENGTH_SHORT).show();
                    }
                    // Establish the Bluetooth socket connection.
                    try {
                        BluetoothService.mBluetoothSocket.connect();
                    } catch (IOException e) {
                        try {
                            fail = true;
                            System.out.println(e.getMessage());
                            BluetoothService.mBluetoothSocket.close();
                        } catch (IOException e2) {
                            Toast.makeText(parent, "Socket creation failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                    if(!fail) {
                        System.out.println("Connected");
                        Intent intent = new Intent("connection_established");
                        intent.putExtra("device", name.equals("null") ? name : address);
                        parent.sendBroadcast(intent);
                    } else {
                        System.out.println("Not Connected!");
                    }
                }
            }.start();
            parent.finish();
        }
    }

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;

        public ViewHolder(View view) {
            super(view);
            // Define click listener for the ViewHolder's View
            textView = view.findViewById(R.id.textView);
        }

        public TextView getTextView() {
            return textView;
        }
    }

    /**
     * Initialize the Adapter.
     */
    public DeviceListAdapter(DeviceList parent) {
        localDataSet = parent.deviceList;
        this.parent = parent;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view, which defines the UI of the list item
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.text_row_item, viewGroup, false);

        view.setOnClickListener(mOnClickListener);

        return new ViewHolder(view);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        viewHolder.getTextView().setText(localDataSet.get(position));
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        try {
            return device.createInsecureRfcommSocketToServiceRecord(BT_MODULE_UUID);
        } catch (Exception e) {
            System.out.println("Could not create connection");
        }
        return  device.createRfcommSocketToServiceRecord(BT_MODULE_UUID);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return localDataSet.size();
    }
}


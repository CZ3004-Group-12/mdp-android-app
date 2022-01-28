package com.example.mdpcontroller;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.ViewHolder> {
    private final List<String> localDataSet;
    private final View.OnClickListener mOnClickListener = new BluetoothOnClickListener();
    private final DeviceList parent;
    private boolean connectedDeviceStr;


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
            Map<String, String> extraMap = new HashMap<>();
            extraMap.put("address", address);
            extraMap.put("name", name);
            BluetoothService.setBtStatus(BluetoothService.BluetoothStatus.CONNECTING, extraMap, parent);
            parent.finish();
        }
    }

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;
        private final TextView deviceConnectedTextView;

        public ViewHolder(View view) {
            super(view);
            // Define click listener for the ViewHolder's View
            textView = view.findViewById(R.id.textView);
            deviceConnectedTextView = view.findViewById(R.id.textViewDeviceConnected);
        }

        public TextView getTextView() {
            return textView;
        }
        public TextView getDeviceConnectedTextView() {
            return deviceConnectedTextView;
        }
    }

    /**
     * Initialize the Adapter.
     */
    public DeviceListAdapter(DeviceList parent) {
        localDataSet = parent.deviceList;
        connectedDeviceStr = false;
        this.parent = parent;
    }

    public void setConnectedDeviceStr(boolean connectedDeviceStr) {
        this.connectedDeviceStr = connectedDeviceStr;
    }

    // Create new views (invoked by the layout manager)
    @NonNull
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
        if (connectedDeviceStr)viewHolder.getDeviceConnectedTextView().setText(R.string.connected);
    }



    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return localDataSet.size();
    }
}


package com.example.mdpcontroller;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Calendar;

public class MainActivity<ActivityResultLauncher> extends AppCompatActivity {
    private BluetoothService btService;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        BluetoothService.initialize(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ((TextView)findViewById(R.id.editTextTextMultiLine)).setMovementMethod(new ScrollingMovementMethod());
        ((ScrollView)findViewById(R.id.SCROLLER_ID)).fullScroll(View.FOCUS_DOWN);
    }

    public void btConnect_onPress(View view) {
        btService = new BluetoothService(this);
        Intent intent = new Intent(this, DeviceList.class);
        startActivity(intent);
    }

    public void sendMessage(View view) {
        TextView tv = findViewById(R.id.editTextTextPersonName);
        String message = tv.getText().toString();
        btService.write(message);
        tv.setText("");
    }

    // Create a BroadcastReceiver for message_received.
    private final BroadcastReceiver msgReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Calendar c = Calendar.getInstance();
            String message = "\n\n"
                    +c.get(Calendar.HOUR_OF_DAY)+":"
                    +c.get(Calendar.MINUTE)+":"
                    +c.get(Calendar.SECOND)+" - "
                    + intent.getExtras().getString("message");

            ((TextView)findViewById(R.id.editTextTextMultiLine)).append(message);


        }
    };

    // Create a BroadcastReceiver for bt_status_changed.
    private final BroadcastReceiver conReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Button bt = findViewById(R.id.button_connect);
            if (BluetoothService.getBtStatus() == BluetoothService.BluetoothStatus.CONNECTED) {
                btService.startConnectedThread();
                String devName = intent.getStringExtra("device");
                bt.setText(String.format(getResources().getString(R.string.button_bluetooth_connected), devName));
            }
            else if (BluetoothService.getBtStatus() == BluetoothService.BluetoothStatus.UNCONNECTED) {
                bt.setText(R.string.button_bluetooth_unconnected);
                btService.disconnect();
            }
        }
    };

    @Override
    protected void onPause() {
        try {
            unregisterReceiver(msgReceiver);
            unregisterReceiver(conReceiver);
        } catch (Exception e) {
            // already unregistered
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            registerReceiver(msgReceiver, new IntentFilter("message_received"));
            registerReceiver(conReceiver, new IntentFilter("bt_status_changed"));
        } catch (Exception e) {
            // already registered
        }
    }



}
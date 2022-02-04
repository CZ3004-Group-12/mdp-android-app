package com.example.mdpcontroller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.ViewPager;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
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

import com.example.mdpcontroller.arena.ArenaView;
import com.example.mdpcontroller.arena.Robot;
import com.example.mdpcontroller.tab.AppDataModel;
import com.example.mdpcontroller.tab.ExploreTabFragment;
import com.example.mdpcontroller.tab.PathTabFragment;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Calendar;

public class MainActivity<ActivityResultLauncher> extends AppCompatActivity {
    private final String DELIMITER = "/";
    private final boolean DEBUG = false;
    private BluetoothService btService;
    private BluetoothService.BluetoothLostReceiver btLostReceiver;
    private BtStatusChangedReceiver conReceiver;

    private AppDataModel appDataModel;
    private ArenaView arena;
    private PathTabFragment pathFrag;

    TabLayout tabLayout;
    ViewPager tabViewPager;
    MainAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        BluetoothService.initialize(this);
        btService = new BluetoothService(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ((TextView)findViewById(R.id.btMessageTextView)).setMovementMethod(new ScrollingMovementMethod());
        ((ScrollView)findViewById(R.id.SCROLLER_ID)).fullScroll(View.FOCUS_DOWN);
        arena = findViewById(R.id.arena);
        // Pass btService to arena
        arena.setBtService(btService);

        //Tab-Layout
        tabLayout = findViewById(R.id.tabLayout);
        tabViewPager = findViewById(R.id.tabViewPager);
        adapter = new MainAdapter(getSupportFragmentManager());
        adapter.AddFragment(new ExploreTabFragment(), "Explore");
        adapter.AddFragment(new PathTabFragment(), "Path");
        tabViewPager.setAdapter(adapter);
        tabLayout.setupWithViewPager(tabViewPager);

        // Make device discoverable and accept connections
        btService.serverStartListen(this);

        // register event receivers
        registerReceiver(msgReceiver, new IntentFilter("message_received"));
        conReceiver = new BtStatusChangedReceiver(this);
        registerReceiver(conReceiver, new IntentFilter("bt_status_changed"));
        btLostReceiver = btService.new BluetoothLostReceiver(this);
        registerReceiver(btLostReceiver, new IntentFilter("bt_status_changed"));

        //Getting data from fragment and assigning to view variable.
        appDataModel = new ViewModelProvider(this).get(AppDataModel.class);

        appDataModel.getIsSetRobot().observe(this, data -> {
            // Perform an action with the latest item data
            arena.isSetRobot = data;
        });
        appDataModel.getIsSetObstacles().observe(this, data -> {
            arena.isSetObstacles = data;
        });
        registerReceiver(sendMsgReceiver, new IntentFilter("send_msg"));
    }

    //BlueTooth
    public void btConnect_onPress(View view) {
        Intent intent = new Intent(this, DeviceList.class);
        btService.serverStartListen(this);
        startActivity(intent);
    }

    // Create a BroadcastReceiver for message_received.
    private final BroadcastReceiver msgReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String message =  intent.getExtras().getString("message");
            //Categorize received messages
            String[] messageArr = message.split(DELIMITER);
            switch(messageArr[0]){
                // Format: ROBOT/<x>/<y>/<dir>
                case("ROBOT"): {
                    arena.setRobot(Integer.parseInt(messageArr[1]), Integer.parseInt(messageArr[2]), messageArr[3]);
                    break;
                }
                // Format: TARGET/<num>/<id>
                case("TARGET") :{
                    arena.setObstacleImageID(messageArr[1], messageArr[2]);
                    break;
                }
                // Format: STATUS/<msg>
                case("STATUS"): {
                    displayMessage("Status update: " + messageArr[1]);
                    break;
                }
                default: {
                    // Unrecognized command, only display message if in debug mode
                    if (DEBUG) displayMessage("DEBUG: " + messageArr[1]);
                }
            }
        }
    };

    // Create a BroadcastReceiver for send_msg.
    public BroadcastReceiver sendMsgReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent){
            String message = intent.getExtras().getString("message");
            btService.write(message);
        }
    };

    // Update robot position and heading
    private void updateRobot(String x, String y, String dir) {

    }

    // Displays a string in the log TextView, prepends time received as well
    private void displayMessage(String msg) {
        Calendar c = Calendar.getInstance();
        msg = "\n\n"
            +c.get(Calendar.HOUR_OF_DAY)+":"
            +c.get(Calendar.MINUTE)+":"
            +c.get(Calendar.SECOND)+" - "
            +msg;

        ((TextView)findViewById(R.id.btMessageTextView)).append(msg);
    }

    // Create a BroadcastReceiver for bt_status_changed.
    public class BtStatusChangedReceiver extends BroadcastReceiver {
        Activity main;

        public BtStatusChangedReceiver(Activity main) {
            super();
            this.main = main;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Button bt = findViewById(R.id.button_connect);
            if (BluetoothService.getBtStatus() == BluetoothService.BluetoothStatus.CONNECTING) {
                btService.serverStopListen();
                btService.clientConnect(intent.getStringExtra("address"),
                        intent.getStringExtra("name"),
                        main);
                bt.setText(getResources().getString(R.string.button_bluetooth_connecting));
                bt.setBackgroundColor(getResources().getColor(R.color.teal_200));

            }
            else if (BluetoothService.getBtStatus() == BluetoothService.BluetoothStatus.CONNECTED) {
                btService.startConnectedThread();
                btService.serverStopListen();
                String devName = intent.getStringExtra("device");
                bt.setText(String.format(getResources().getString(R.string.button_bluetooth_connected), devName));
                bt.setBackgroundColor(getResources().getColor(R.color.green_500));

            }
            else if (BluetoothService.getBtStatus() == BluetoothService.BluetoothStatus.UNCONNECTED) {
                btService.disconnect();
                btService.serverStartListen(main);
                bt.setText(R.string.button_bluetooth_unconnected);
                bt.setBackgroundColor(getResources().getColor(R.color.purple_200));
            }
            else if (BluetoothService.getBtStatus() == BluetoothService.BluetoothStatus.DISCONNECTED) {
                bt.setText(getResources().getString(R.string.button_bluetooth_disconnected));
                bt.setBackgroundColor(getResources().getColor(R.color.orange_500));
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(msgReceiver);
        unregisterReceiver(conReceiver);
        unregisterReceiver(btLostReceiver);
        unregisterReceiver(sendMsgReceiver);
    }


    //Tab-bar
    private class MainAdapter extends FragmentPagerAdapter {
        ArrayList<Fragment> fragmentArrayList = new ArrayList<>();
        ArrayList<String> stringArrayList = new ArrayList<>();
        public MainAdapter(@NonNull FragmentManager fm) {
            super(fm);
        }
        public void AddFragment(Fragment fragment, String s){
            fragmentArrayList.add(fragment);
            stringArrayList.add(s);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return fragmentArrayList.get(position);
        }

        @Override
        public int getCount() {
            return fragmentArrayList.size();
        }
        @Nullable
        @Override
        public CharSequence getPageTitle(int position){
            return stringArrayList.get(position);
        }
    }



}
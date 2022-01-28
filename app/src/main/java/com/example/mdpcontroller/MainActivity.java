package com.example.mdpcontroller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
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

import com.example.mdpcontroller.tab.ExploreTabFragment;
import com.example.mdpcontroller.tab.PathTabFragment;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Calendar;

public class MainActivity<ActivityResultLauncher> extends AppCompatActivity {
    private BluetoothService btService;
    private BluetoothService.BluetoothLostReceiver btLostReceiver;
    private BtStatusChangedReceiver conReceiver;

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

        // register receivers
        registerReceiver(msgReceiver, new IntentFilter("message_received"));
        conReceiver = new BtStatusChangedReceiver(this);
        registerReceiver(conReceiver, new IntentFilter("bt_status_changed"));
        btLostReceiver = btService.new BluetoothLostReceiver(this);
        registerReceiver(btLostReceiver, new IntentFilter("bt_status_changed"));
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
            Calendar c = Calendar.getInstance();
            String message = "\n\n"
                    +c.get(Calendar.HOUR_OF_DAY)+":"
                    +c.get(Calendar.MINUTE)+":"
                    +c.get(Calendar.SECOND)+" - "
                    + intent.getExtras().getString("message");

            ((TextView)findViewById(R.id.btMessageTextView)).append(message);
        }
    };

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
            System.out.println("on receive");
            if (BluetoothService.getBtStatus() == BluetoothService.BluetoothStatus.CONNECTING) {
                btService.serverStopListen();
                btService.clientConnect(intent.getStringExtra("address"),
                        intent.getStringExtra("name"),
                        main);
                System.out.println("Fin");
            }
            else if (BluetoothService.getBtStatus() == BluetoothService.BluetoothStatus.CONNECTED) {
                btService.startConnectedThread();
                btService.serverStopListen();
                String devName = intent.getStringExtra("device");
                bt.setText(String.format(getResources().getString(R.string.button_bluetooth_connected), devName));

            }
            else if (BluetoothService.getBtStatus() == BluetoothService.BluetoothStatus.UNCONNECTED) {
                bt.setText(R.string.button_bluetooth_unconnected);
                btService.disconnect();
                btService.serverStartListen(main);
            }
            else if (BluetoothService.getBtStatus() == BluetoothService.BluetoothStatus.DISCONNECTED) {
                bt.setText(getResources().getString(R.string.button_bluetooth_disconnected));
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(msgReceiver);
        unregisterReceiver(conReceiver);
        unregisterReceiver(btLostReceiver);
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
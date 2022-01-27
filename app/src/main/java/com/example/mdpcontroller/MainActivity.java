package com.example.mdpcontroller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

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

    TabLayout tabLayout;
    ViewPager tabViewPager;
    MainAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        BluetoothService.initialize(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        ((TextView)findViewById(R.id.editTextTextMultiLine)).setMovementMethod(new ScrollingMovementMethod());
//        ((ScrollView)findViewById(R.id.SCROLLER_ID)).fullScroll(View.FOCUS_DOWN);

        //Tab-Layout
        tabLayout = findViewById(R.id.tabLayout);
        tabViewPager = findViewById(R.id.tabViewPager);
        adapter = new MainAdapter(getSupportFragmentManager());
        adapter.AddFragment(new ExploreTabFragment(), "Explore");
        adapter.AddFragment(new PathTabFragment(), "Path");
        tabViewPager.setAdapter(adapter);
        tabLayout.setupWithViewPager(tabViewPager);
    }

    //BlueTooth

    public void btConnect_onPress(View view) {
        btService = new BluetoothService(this);
        Intent intent = new Intent(this, DeviceList.class);
        startActivity(intent);
    }

//    public void sendMessage(View view) {
//        TextView tv = findViewById(R.id.editTextTextPersonName);
//        String message = tv.getText().toString();
//        btService.write(message);
//        tv.setText("");
//    }

//    // Create a BroadcastReceiver for message_received.
//    private final BroadcastReceiver msgReceiver = new BroadcastReceiver() {
//        public void onReceive(Context context, Intent intent) {
//            Calendar c = Calendar.getInstance();
//            String message = "\n\n"
//                    +c.get(Calendar.HOUR_OF_DAY)+":"
//                    +c.get(Calendar.MINUTE)+":"
//                    +c.get(Calendar.SECOND)+" - "
//                    + intent.getExtras().getString("message");
//
//            ((TextView)findViewById(R.id.editTextTextMultiLine)).append(message);
//
//
//        }
//    };

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
            else if (BluetoothService.getBtStatus() == BluetoothService.BluetoothStatus.DISCONNECTED) {
                String devName = intent.getStringExtra("device");
                bt.setText(getResources().getString(R.string.button_bluetooth_disconnected));
            }
        }
    };

//    @Override
//    protected void onPause() {
//        try {
//            unregisterReceiver(msgReceiver);
//            unregisterReceiver(conReceiver);
//            unregisterReceiver(btLostReceiver);
//        } catch (Exception e) {
//            // already unregistered
//        }
//        super.onPause();
//    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//        try {
//            registerReceiver(msgReceiver, new IntentFilter("message_received"));
//            registerReceiver(conReceiver, new IntentFilter("bt_status_changed"));
//            btLostReceiver = btService.new BluetoothLostReceiver(this);
//            registerReceiver(btLostReceiver, new IntentFilter("bt_status_changed"));
//        } catch (Exception e) {
//            // already registered
//        }
//    }

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
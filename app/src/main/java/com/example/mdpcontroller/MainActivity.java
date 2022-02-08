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
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;

import com.example.mdpcontroller.arena.ArenaView;
import com.example.mdpcontroller.arena.Robot;
import com.example.mdpcontroller.tab.AppDataModel;
import com.example.mdpcontroller.tab.ExploreTabFragment;
import com.example.mdpcontroller.tab.ManualTabFragment;
import com.example.mdpcontroller.tab.PathTabFragment;
import com.example.mdpcontroller.tab.SettingsTabFragment;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity<ActivityResultLauncher> extends AppCompatActivity {
    private final String DELIMITER = "/";
    public boolean DEBUG = false;
    public boolean RUN_TO_END = false;
    private BluetoothService btService;
    private BluetoothService.BluetoothLostReceiver btLostReceiver;
    private BtStatusChangedReceiver conReceiver;

    private AppDataModel appDataModel;
    private ArenaView arena;
    private PathTabFragment pathFrag;
    private ExploreTabFragment exploreTabFragment;
    private PathTabFragment pathTabFragment;

    // for timer
    private final Handler timerHandler  = new Handler();
    TimerRunnable timerRunnable = null;

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
        adapter.AddFragment(new ManualTabFragment(), "Manual");
        adapter.AddFragment(new SettingsTabFragment(), "Settings");
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

        displayMessage("Status updates will appear here");
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
            try{
                switch(messageArr[0]){
                    // Format: ROBOT/<x>/<y>/<dir>
                    case("ROBOT"): {
                        int xCoord = Integer.parseInt(messageArr[1]);
                        int yCoord = ArenaView.ROWS-1-Integer.parseInt(messageArr[2]);
                        arena.setRobot(xCoord, yCoord, messageArr[3]);
                        break;
                    }
                    // Format: TARGET/<num>/<id>
                    case("TARGET") :{
                        arena.setObstacleImageID(messageArr[1], messageArr[2]);
                        break;
                    }
                    // Format: STATUS/<msg>
                    case("STATUS"): {
                        displayMessage("Status update\n" + messageArr[1]);
                        break;
                    }
                    // Format: DEBUG/<msg>
                    case("DEBUG"): {
                        if (DEBUG) displayMessage("DEBUG\n" + messageArr[1]);
                        break;
                    }
                    default: {
                        displayMessage("ERROR (Unrecognized command)\n" + messageArr[0]);
                    }
                }
            } catch(IndexOutOfBoundsException e){
                // message incorrect message parameters
                displayMessage("ERROR (Incorrect message format)\n" + message);
            }

        }
    };

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
                displayMessage("Status update\nConnected");
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
                if (BluetoothService.RECONNECT_AS_CLIENT) displayMessage("Status update\nDisconnected, attempting to reconnect...");
                else displayMessage("Status update\nDisconnected, waiting for reconnect...");
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(msgReceiver);
        unregisterReceiver(conReceiver);
        unregisterReceiver(btLostReceiver);
        timerHandler.removeCallbacks(timerRunnable);
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

    public void moveBtn(View view){
        ImageButton pressedBtn = (ImageButton) view;
        String dir;
        switch (pressedBtn.getId()){
            case(R.id.fButton): {
                dir = "F";
                break;
            }
            case(R.id.rButton): {
                dir = "R";
                break;
            }
            case(R.id.lButton): {
                dir = "L";
                break;
            }
            case(R.id.bButton): {
                dir = "B";
                break;
            }
            case(R.id.brButton): {
                dir = "BR";
                break;
            }
            case(R.id.blButton): {
                dir = "BL";
                break;
            }
            default: return;
        }
        btService.write(String.format("MOVE/%s", dir));
    }

    public void clearObstacles(View view){
        arena.clearObstacles();
    }

    public void toggleDebugMode(View view){
        DEBUG = !DEBUG;
        ((CheckedTextView)findViewById(R.id.toggleDebug)).setChecked(DEBUG);
    }

    public void toggleReconnectAsClient(View view){
        BluetoothService.RECONNECT_AS_CLIENT = !BluetoothService.RECONNECT_AS_CLIENT;
        ((CheckedTextView)findViewById(R.id.toggleReconnectAsClient)).setChecked(BluetoothService.RECONNECT_AS_CLIENT);
    }

    public void toggleRunToEnd(View view){
        RUN_TO_END = !RUN_TO_END;
        ((CheckedTextView)findViewById(R.id.toggleRunToEnd)).setChecked(RUN_TO_END);
    }

    private class TimerRunnable implements Runnable {
        private TextView timerTextView;
        private long startTime = 0;
        public TimerRunnable(TextView timerTextView){
            this.timerTextView = timerTextView;
        }
        @Override
        public void run(){
            long millis = System.currentTimeMillis() - startTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;

            timerTextView.setText(String.format(Locale.getDefault(),"%02d:%02d", minutes, seconds));

            timerHandler.postDelayed(this, 500);
        }
    }

    public void startStopTimer(View view){
        Button b = (Button)view;
        if (timerRunnable != null) { // timer was running, reset the timer and send stop command
            if (b.getId() == R.id.startExplore){
                b.setText(R.string.start_explore);
            } else {
                b.setText(R.string.start_fastest_path);
            }
            timerHandler.removeCallbacks(timerRunnable);
            timerRunnable.timerTextView.setText(R.string.time_placeholder);
            timerRunnable = null;
            toggleActivateButtons(true);
            btService.write("STOP");
        }
        else{ // start timer
            String cmd;
            if (b.getId() == R.id.startExplore){
                timerRunnable = new TimerRunnable(findViewById(R.id.timerTextViewExplore));
                cmd = "STARTEXPLORE";
                b.setText(R.string.stop_explore);
            } else {
                timerRunnable = new TimerRunnable(findViewById(R.id.timerTextViewPath));
                cmd = "STARTPATH";
                b.setText(R.string.stop_fastest_path);
            }
            timerRunnable.startTime = System.currentTimeMillis();
            timerHandler.postDelayed(timerRunnable, 0);
            btService.write(cmd);
            toggleActivateButtons(false);
        }
    }

    private void toggleActivateButtons(boolean val){
        // deactivate obstacle and robot setting when robot is moving
        appDataModel.setIsSetObstacles(false);
        appDataModel.setIsSetRobot(false);
        findViewById(R.id.setObstacles).setEnabled(val);
        findViewById(R.id.setRobot).setEnabled(val);
        findViewById(R.id.clearObstacles).setEnabled(val);
        if (RUN_TO_END) {
            btService.allowWrite = val; // block all outward communication to robot
            findViewById(R.id.startExplore).setEnabled(val);
            findViewById(R.id.startPath).setEnabled(val);
        }
    }

}
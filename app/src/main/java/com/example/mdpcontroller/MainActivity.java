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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mdpcontroller.arena.ArenaView;
import com.example.mdpcontroller.arena.Cell;
import com.example.mdpcontroller.arena.Obstacle;
import com.example.mdpcontroller.arena.ObstacleDialogueFragment;
import com.example.mdpcontroller.arena.Robot;
import com.example.mdpcontroller.tab.AppDataModel;
import com.example.mdpcontroller.tab.ExploreTabFragment;
import com.example.mdpcontroller.tab.ManualTabFragment;
import com.example.mdpcontroller.tab.PathTabFragment;
import com.example.mdpcontroller.tab.SettingsTabFragment;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Locale;

public class MainActivity<ActivityResultLauncher> extends AppCompatActivity implements ObstacleDialogueFragment.DialogDataListener {
    private final String DELIMITER = "/";
    public boolean DEBUG = false;
    public boolean RUN_TO_END = false;
    private BluetoothService btService;
    private BluetoothService.BluetoothLostReceiver btLostReceiver;
    private BtStatusChangedReceiver conReceiver;

    private AppDataModel appDataModel;
    private ArenaView arena;
    private List<String> moveList;
    // for timer
    private final Handler timerHandler  = new Handler();
    TimerRunnable timerRunnable = null;
    private String curObsNum = "0";

    TabLayout tabLayout;
    ViewPager tabViewPager;
    MainAdapter adapter;

    public String robotDir;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        BluetoothService.initialize(this);
        btService = new BluetoothService(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ((TextView)findViewById(R.id.btMessageTextView)).setMovementMethod(new ScrollingMovementMethod());
        ((ScrollView)findViewById(R.id.SCROLLER_ID)).fullScroll(View.FOCUS_DOWN);
        arena = findViewById(R.id.arena);

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

        appDataModel = new ViewModelProvider(this).get(AppDataModel.class);

        appDataModel.getIsSetRobot().observe(this, data -> {
            arena.isSetRobot = data;
        });
        appDataModel.getIsSetObstacles().observe(this, data -> {
            arena.isSetObstacles = data;
        });
        displayMessage("Status updates will appear here");
        arena.setEventListener(new ArenaView.DataEventListener() {
            @Override
            public void onEventOccurred() {
                if(arena.obstacleEdit){
                    showObstacleDialog();
                }
            }
        });
        robotDir = "N";

        moveList = new ArrayList<>();
        setConnectBtn();
    }

    //BlueTooth
    public void btConnect_onPress(View view) {
        // connect as server
        if (!BluetoothService.CONNECT_AS_CLIENT && !(BluetoothService.getBtStatus() == BluetoothService.BluetoothStatus.CONNECTED)) {
            btService.serverStartListen(this);
        }
        // connect as client
        else{
            Intent intent = new Intent(this, DeviceList.class);
            startActivity(intent);
        }
    }

    // Create a BroadcastReceiver for message_received.
    private final BroadcastReceiver msgReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String fullMessage =  intent.getExtras().getString("message");
            if (DEBUG) displayMessage(fullMessage);
            if (fullMessage.length() ==0) return;
            //Categorize received messages
            if (fullMessage.charAt(0) == '&') fullMessage = fullMessage.substring(1);
            String[] commandArr = fullMessage.split("&");
            for (String message: commandArr) {
                try {
                    String[] messageArr = message.split("\\|"); // remove header
                    if (messageArr.length > 1) messageArr = messageArr[1].split(DELIMITER);
                    else messageArr = messageArr[0].split(DELIMITER);
                    switch (messageArr[0]) {
                        // Format: ROBOT/<dims>/<posList>
                        case ("ROBOT"): {
                            if (messageArr.length < 3) break;
                            TextView obstacleStatus = findViewById(R.id.obstacleStatusTextView);
                            int xCoord = Integer.parseInt(messageArr[1].split("-")[0]);
                            int yCoord = ArenaView.ROWS - 1 - Integer.parseInt(messageArr[1].split("-")[1]);
                            curObsNum = arena.findObstacle(xCoord, yCoord);
                            if (obstacleStatus != null) {
                                obstacleStatus.setText("Status: Searching for obstacle " + curObsNum);
                                displayMessage("Status update\nSearching for obstacle " + curObsNum);
                            }
                            moveList.clear();
                            moveList.addAll(Arrays.asList(messageArr).subList(2, messageArr.length));
                            displayMessage("Status update\n" + "Movement Started");
                            break;
                        }
                        case ("DONE"): {
                            int numInst = Integer.parseInt(messageArr[1]);
                            if (moveList.size() < numInst || moveList.size() == 0) break;
                            new Thread() {
                                public void run() {
                                    String prevDir = null;
                                    for (int i = 0; i < numInst; i++) {
                                        String[] args = moveList.get(0).replaceAll("\\(|\\)", "").split(",");
                                        int xCoord = (int) Double.parseDouble(args[0].trim());
                                        int yCoord = ArenaView.ROWS - 1 - (int) Double.parseDouble(args[1].trim());
                                        String dir = "N";
                                        switch (args[2].trim()) {
                                            case ("0"):
                                                dir = "N";
                                                break;
                                            case ("90"):
                                                dir = "W";
                                                break;
                                            case ("-90"):
                                                dir = "E";
                                                break;
                                            case ("180"):
                                                dir = "S";
                                                break;
                                        }
                                        arena.setRobot(xCoord, yCoord, dir);
                                        Intent intent = new Intent("message_received");
                                        intent.putExtra("message", "ROBOT_STATUS/"+String.format(Locale.getDefault(), "Position: (%3d,%3d, %s )", xCoord, yCoord, dir));
                                        context.sendBroadcast(intent);
                                        moveList.remove(0);
                                        try {
                                            if (dir.equals(prevDir)){
                                                Thread.sleep(1000);
                                            }
                                            else{
                                                Thread.sleep(3000);
                                            }
                                            prevDir = dir;
                                        } catch (InterruptedException e) {
                                            System.out.println("Interrupted");
                                        }
                                    }
                                }
                            }.start();
                            break;
                        }
                        // Format: TARGET/<num>/<id>
                        case ("TARGET"): {
                            try {
                                arena.setObstacleImageID(curObsNum, messageArr[1]);
                            } catch (Exception e) {
                                if (DEBUG) {
                                    displayMessage("DEBUG\nInvalid message: " + message);
                                }
                                System.out.println("Invalid imageID or obstacleID: " + message);
                            }
                            break;
                        }
                        case("ROBOT_STATUS"):{
                            TextView robotPosTextView = findViewById(R.id.robotPosTextView);
                            robotPosTextView.setText(messageArr[1]);
                            break;
                        }
                        // Format: STATUS/<msg>
                        case ("STATUS"): {
                            displayMessage("Status update\n" + messageArr[1]);
                            break;
                        }
                        // Format: DEBUG/<msg>
                        case ("DEBUG"): {
                            if (DEBUG) displayMessage("DEBUG\n" + messageArr[1]);
                            break;
                        }
                        case ("FINISH"): {
                            if (messageArr[1].equals("EXPLORE")) {
                                startStopTimer(findViewById(R.id.startExplore));
                                String timeTaken = ((TextView) findViewById(R.id.timerTextViewExplore)).getText().toString();
                                displayMessage("Status update\n" + "Exploration complete!\nTime taken: " + timeTaken);
                            } else {
                                startStopTimer(findViewById(R.id.startPath));
                                String timeTaken = ((TextView) findViewById(R.id.timerTextViewExplore)).getText().toString();
                                displayMessage("Status update\n" + "Fastest path complete!\nTime taken: " + timeTaken);
                            }
                            break;
                        }
                        default: {
                            displayMessage("ERROR (Unrecognized command)\n" + messageArr[0]);
                        }
                    }
                } catch (Exception e) {
                    // message incorrect message parameters
                    displayMessage("ERROR (" + e.getMessage() + ")\n" + message);
                }
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
            }
            else if (BluetoothService.getBtStatus() == BluetoothService.BluetoothStatus.CONNECTED) {
                btService.startConnectedThread();
                btService.serverStopListen();
                displayMessage("Status update\nConnected");
            }
            else if (BluetoothService.getBtStatus() == BluetoothService.BluetoothStatus.UNCONNECTED) {
                btService.disconnect();
                btService.serverStartListen(main);
            }
            else if (BluetoothService.getBtStatus() == BluetoothService.BluetoothStatus.DISCONNECTED) {
                if (BluetoothService.CONNECT_AS_CLIENT) displayMessage("Status update\nDisconnected, attempting to reconnect...");
                else displayMessage("Status update\nDisconnected, waiting for reconnect...");
            }
            setConnectBtn();
        }
    };

    public void setConnectBtn(){
        Button bt = findViewById(R.id.button_connect);
        if (BluetoothService.getBtStatus() == BluetoothService.BluetoothStatus.CONNECTING) {
            bt.setText(getResources().getString(R.string.button_bluetooth_connecting));
            bt.setBackgroundColor(getResources().getColor(R.color.teal_200));
        }
        else if (BluetoothService.getBtStatus() == BluetoothService.BluetoothStatus.CONNECTED) {
            String name = BluetoothService.mConnectedDevice.getName()!=null?BluetoothService.mConnectedDevice.getName(): BluetoothService.mConnectedDevice.getAddress();
            bt.setText(String.format(getResources().getString(R.string.button_bluetooth_connected), name));
            bt.setBackgroundColor(getResources().getColor(R.color.green_500));
        }
        else if (BluetoothService.getBtStatus() == BluetoothService.BluetoothStatus.UNCONNECTED) {
            bt.setText(R.string.button_bluetooth_unconnected);
            bt.setBackgroundColor(getResources().getColor(R.color.purple_200));
        }
        else if (BluetoothService.getBtStatus() == BluetoothService.BluetoothStatus.DISCONNECTED) {
            bt.setText(getResources().getString(R.string.button_bluetooth_disconnected));
            bt.setBackgroundColor(getResources().getColor(R.color.orange_500));
        }
    }

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
        if(Robot.robotMatrix[0][0] == null){
            System.out.println("Robot is not set up on map");
        }else{
            btService.write(String.format("MOVE/%s", dir));
            if(robotDir != null){ //just to catch error
                robotDir = arena.moveRobot(robotDir,dir);
            }
        }
    }

    public void clearObstacles(View view){
        arena.clearObstacles();
    }

    public void toggleDebugMode(View view){
        DEBUG = !DEBUG;
        ((CheckedTextView)findViewById(R.id.toggleDebug)).setChecked(DEBUG);
    }

    public void toggleReconnectAsClient(View view){
        BluetoothService.CONNECT_AS_CLIENT = !BluetoothService.CONNECT_AS_CLIENT;
        if (!BluetoothService.CONNECT_AS_CLIENT) btService.serverStartListen(this);
        ((CheckedTextView)findViewById(R.id.toggleReconnectAsClient)).setChecked(BluetoothService.CONNECT_AS_CLIENT);
    }

    public void toggleRunToEnd(View view){
        RUN_TO_END = !RUN_TO_END;
        ((CheckedTextView)findViewById(R.id.toggleRunToEnd)).setChecked(RUN_TO_END);
    }

    private class TimerRunnable implements Runnable {
        private final TextView timerTextView;
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
        if (BluetoothService.getBtStatus() == BluetoothService.BluetoothStatus.CONNECTED) {
            Button b = (Button) view;
            if (timerRunnable != null) { // timer was running, reset the timer and send stop command
                if (b.getId() == R.id.startExplore) {
                    b.setText(R.string.start_explore);
                } else {
                    b.setText(R.string.start_fastest_path);
                }
                timerHandler.removeCallbacks(timerRunnable);
                timerRunnable = null;
                toggleActivateButtons(true);
                btService.write("STOP");
            } else { // start timer
                if (b.getId() == R.id.startExplore) {
                    if(arena.obstacles.size() > 0){
                        for(Obstacle obstacle: arena.obstacles){
                            obstacle.explored = false;
                            obstacle.imageID = "-1";
                        }
                        arena.invalidate();
                    }
                    timerRunnable = new TimerRunnable(findViewById(R.id.timerTextViewExplore));
                    curObsNum = "0";
                    b.setText(R.string.stop_explore);
                    Cell curCell;
                    int xCoord, yCoord;
                    String dir = "0";
                    StringBuilder cmd = new StringBuilder("START/EXPLORE");

                    // Robot position
                    if (Robot.robotMatrix[1][1] != null) {
                        Cell center = Robot.robotMatrix[1][1];
                        cmd.append(String.format(Locale.getDefault(),"/(R,%02d,%02d,0)", center.col, ArenaView.ROWS-1-center.row));
                    }
                    else cmd.append("/(R,01,01,0)");

                    // Obstacle position
                    for (int i = 0; i < arena.obstacles.size(); i++) {
                        switch(arena.obstacles.get(i).imageDir){
                            case("TOP"): dir = "0"; break;
                            case("LEFT"): dir = "90"; break;
                            case("RIGHT"): dir = "-90"; break;
                            case("BOTTOM"): dir = "180"; break;
                        }
                        curCell = arena.obstacles.get(i).cell;
                        xCoord = curCell.col;
                        yCoord = ArenaView.ROWS - 1 - curCell.row; // invert y coordinates since algorithm uses bottom left as origin
                        cmd.append(String.format(Locale.getDefault(), "/(%02d,%02d,%02d,%s)", i, xCoord, yCoord, dir));
                    }
                    btService.write(cmd.toString());
                } else {
                    timerRunnable = new TimerRunnable(findViewById(R.id.timerTextViewPath));
                    btService.write("START/PATH");
                    b.setText(R.string.stop_fastest_path);
                }
                timerRunnable.startTime = System.currentTimeMillis();
                timerHandler.postDelayed(timerRunnable, 0);
                toggleActivateButtons(false);
            }
        } else {
            // disable start task if bluetooth not connected
            Toast.makeText(this, "Bluetooth not connected!", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleActivateButtons(boolean val){
        // deactivate obstacle and robot setting when robot is moving
        if (appDataModel.getIsSetObstacles().getValue()){
            findViewById(R.id.setObstacles).callOnClick();
        }
        if (appDataModel.getIsSetRobot().getValue()){
            findViewById(R.id.setRobot).callOnClick();
        }
        findViewById(R.id.setObstacles).setEnabled(val);
        findViewById(R.id.setRobot).setEnabled(val);
        findViewById(R.id.clearObstacles).setEnabled(val);
        // disable tabs
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        LinearLayout tabStrip = ((LinearLayout)tabLayout.getChildAt(0));
        tabStrip.setEnabled(val);
        for(int i = 0; i < tabStrip.getChildCount(); i++) {
            tabStrip.getChildAt(i).setClickable(val);
        }
        if (RUN_TO_END) {
            findViewById(R.id.startExplore).setEnabled(val);
            findViewById(R.id.startPath).setEnabled(val);
        }
    }
    private void showObstacleDialog() {
        int obsIndex = arena.obstacles.indexOf(arena.editingObs);
        int obsX = arena.editingObs.getCell().getCol();
        int obsY = arena.editingObs.getCell().getRow();
        String imageDir=arena.editingObs.getImageDir();;
        String imageID;
        if(arena.editingObs.explored == false){
             imageID=Integer.toString(obsIndex+1);
        }else{
            imageID = arena.editingObs.getImageID();
        }

        ObstacleDialogueFragment obstacleDialogueFragment =
                ObstacleDialogueFragment.newInstance(obsIndex,imageID,imageDir,obsX,obsY);
        obstacleDialogueFragment.show(getFragmentManager(),"hello");
        obstacleDialogueFragment.setCancelable(false);
    }
    @Override
    public void dialogData(int obsIndex, String imageDir, int x, int y) {
        Cell curCell;
        RectF curRect;
        for (Map.Entry<Cell, RectF> entry : arena.gridMap.entrySet()) {
            curCell = entry.getKey();
            curRect = entry.getValue();

            if(curCell.getCol() == x && curCell.getRow() == y && curCell.getType() == "obstacle" ){
                curCell.setType("");
            }
            if(curCell.getCol() == x && curCell.getRow() == y && curCell.getType() == ""){
                curCell.setType("obstacle");
                arena.obstacles.get(obsIndex).setImageDir(imageDir);
                arena.obstacles.get(obsIndex).setCell(curCell);
            }else if(curCell.getCol() == x && curCell.getRow() == y && curCell.getType() != ""){
                arena.obstacles.get(obsIndex).setImageDir(imageDir);
                Toast toast = Toast.makeText(getApplicationContext(),
                        "Grid is already occupied",
                        Toast.LENGTH_SHORT);

                toast.show();
            }
        }
        arena.invalidate();
    }
    @Override
    public void setObstacleEdit(boolean obsEdit) {
        arena.obstacleEdit = obsEdit;
    }
    @Override
    public void onPause(){
        super.onPause();
        if (arena == null) return;
        BluetoothService.obstacles = arena.obstacles;
        BluetoothService.cells = arena.cells;
    }

    @Override
    public void onResume(){
        super.onResume();
        if (BluetoothService.obstacles == null) return;
        arena.obstacles = BluetoothService.obstacles;
        arena.cells = BluetoothService.cells;
    }

}
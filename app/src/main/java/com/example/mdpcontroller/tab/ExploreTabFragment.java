package com.example.mdpcontroller.tab;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.example.mdpcontroller.BluetoothService;
import com.example.mdpcontroller.MainActivity;
import com.example.mdpcontroller.R;


public class ExploreTabFragment extends Fragment {
    View view;
    private Button setRobotBtn;
    private Button setObstaclesBtn;

    private boolean isSetRobot;
    private boolean isSetObstacles;
    private boolean isRobotMove;
    private boolean isRobotStop;
    private boolean isReset;
    private AppDataModel appDataModel;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        isSetRobot = false;
        isSetObstacles = false;
        isRobotMove = false;
        isRobotStop = false;
        isReset = false;
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_explore_tab, container, false);
        setRobotBtn = view.findViewById(R.id.setRobot);
        setObstaclesBtn = view.findViewById(R.id.setObstacles);
        appDataModel = new ViewModelProvider(requireActivity()).get(AppDataModel.class);

        setRobotBtn.setOnClickListener(item ->{
            if(isSetObstacles){
                isSetObstacles = false;
                setObstaclesBtn.setText("Set Obstacles");
                isSetRobot = btnAction(isSetRobot, setRobotBtn, "robot");
            }else{
                isSetRobot = btnAction(isSetRobot, setRobotBtn, "robot");
            }
            appDataModel.setIsSetObstacles(isSetObstacles);
            appDataModel.setIsSetRobot(isSetRobot);
        });
        setObstaclesBtn.setOnClickListener(item ->{
            if(isSetRobot){
                isSetRobot = false;
                setRobotBtn.setText("Set Robot");
                isSetObstacles = btnAction(isSetObstacles, setObstaclesBtn, "obstacles");
            }else{
                isSetObstacles = btnAction(isSetObstacles, setObstaclesBtn, "obstacles");
            }

            appDataModel.setIsSetObstacles(isSetObstacles);
            appDataModel.setIsSetRobot(isSetRobot);
        });

        return view;
    }

    private boolean btnAction(boolean btnVal, Button btn, String btnText){
        // ensure that obstacles and robot are only set when bluetooth is connected
        if (BluetoothService.getBtStatus() != BluetoothService.BluetoothStatus.CONNECTED){
            Toast.makeText(getContext(), "Bluetooth not connected!", Toast.LENGTH_SHORT).show();
            return false;
        }
        if(btnVal){
            btnVal = false;
            btn.setText("Set " + btnText);
        }
        else{
            btnVal = true;
            btn.setText("Done");
        }
        return btnVal;
    }
}
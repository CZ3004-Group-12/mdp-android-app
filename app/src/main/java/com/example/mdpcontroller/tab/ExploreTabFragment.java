package com.example.mdpcontroller.tab;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.mdpcontroller.MainActivity;
import com.example.mdpcontroller.R;
import com.example.mdpcontroller.arena.ArenaView;


public class ExploreTabFragment extends Fragment {
    View view;
    private Button setRobotBtn;
    private Button setObstaclesBtn;

    private boolean isSetRobot = false;
    private boolean isSetObstacles = false;
    private boolean isRobotMove = false;
    private boolean isRobotStop = false;
    //back to starting position
    private boolean isReset = false;

    private AppStateViewModel appStateViewModel;

//    OnDataPass dataPasser;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_explore_tab, container, false);
        setRobotBtn = view.findViewById(R.id.setRobot);
        setObstaclesBtn = view.findViewById(R.id.setObstacles);
        appStateViewModel = new ViewModelProvider(requireActivity()).get(AppStateViewModel.class);

        setRobotBtn.setOnClickListener(item ->{
            if(isSetRobot == true){
                isSetRobot = false;
                setRobotBtn.setText("Set Robot");
            }
            else{
                isSetRobot = true;
                setRobotBtn.setText("Done");
            }
//            tabBtnClick(isSetRobot);

            appStateViewModel.setRobot(isSetRobot);
        });
        setObstaclesBtn.setOnClickListener(item ->{
            if(isSetObstacles == true){
                isSetObstacles = false;
                setObstaclesBtn.setText("Set Obstacles");
            }
            else{
                isSetObstacles = true;
                setObstaclesBtn.setText("Done");
            }

            appStateViewModel.setObstacles(isSetObstacles);
        });

        return view;
    }
}
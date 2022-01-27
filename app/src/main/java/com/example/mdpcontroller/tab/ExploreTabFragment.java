package com.example.mdpcontroller.tab;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.mdpcontroller.MainActivity;
import com.example.mdpcontroller.R;


public class ExploreTabFragment extends Fragment {
    View view;
    private Button setRobotBtn;
    private Button setObstaclesBtn;


    private Boolean setRobot = false;
    private Boolean setObstacles = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_explore_tab, container, false);
        setRobotBtn = view.findViewById(R.id.setRobot);
        setObstaclesBtn = view.findViewById(R.id.setObstacles);

        setRobotBtn.setOnClickListener(view -> {
            if(setRobot == false){
                setRobot = true;
                setRobotBtn.setText("Done");
            }else{
                setRobot = false;
                setRobotBtn.setText("Set robot");
            }
            Intent i = new Intent(getActivity(), MainActivity.class);
            i.putExtra("SET_ROBOT",setRobot);
            startActivity(i);
        });

        return view;
    }
}
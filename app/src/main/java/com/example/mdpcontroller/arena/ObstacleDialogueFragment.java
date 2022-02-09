package com.example.mdpcontroller.arena;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.example.mdpcontroller.MainActivity;
import com.example.mdpcontroller.R;

import java.util.ArrayList;

public class ObstacleDialogueFragment extends android.app.DialogFragment{

    private RelativeLayout relativeLayout;
    private Spinner  imageDir;
    private EditText xPos,yPos;
    private Button cancelBtn, submitBtn;
    private ObstacleEditDrawing obstacleView;
    String[] Directions = { "TOP", "LEFT", "RIGHT", "BOTTOM" };
    public static ObstacleDialogueFragment newInstance(int obsIndex,String imageID, String imageDir, int x, int y) {
        ObstacleDialogueFragment dialog = new ObstacleDialogueFragment();
        Bundle bundle = new Bundle();
        bundle.putString("OBSDIR",imageDir);
        bundle.putString("OBSID",imageID);
        bundle.putInt("OBSX",x);
        bundle.putInt("OBSY",y);
        bundle.putInt("OBSINDEX",obsIndex);
        dialog.setArguments(bundle);
        return dialog;
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
//        obstacleView = new ObstacleEditDrawing(getActivity());
        return inflater.inflate(R.layout.obstacle_pop_up, container,false);
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        relativeLayout = view.findViewById(R.id.obstacleCanvas);
        obstacleView = new ObstacleEditDrawing(getActivity());
        relativeLayout.addView(obstacleView);

        cancelBtn = view.findViewById(R.id.cancelBtn);
        submitBtn = view.findViewById(R.id.submitBtn);
        xPos = view.findViewById(R.id.xPos);
        obstacleView.x = getArguments().getInt("OBSX");
        xPos.setText(obstacleView.x.toString());
        yPos = view.findViewById(R.id.yPos);
        obstacleView.y = getArguments().getInt("OBSY");
        yPos.setText(obstacleView.y.toString());

        imageDir = view.findViewById(R.id.imageDirSpinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_spinner_dropdown_item, Directions);
        adapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        imageDir.setAdapter(adapter);
        String spinnerVal = getArguments().getString("OBSDIR");
        int spinnerPos = adapter.getPosition(spinnerVal);
        imageDir.setSelection(spinnerPos);
        obstacleView.imageDir = spinnerVal;
        obstacleView.imageID = Integer.parseInt(getArguments().getString("OBSID"));
        imageDir.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                obstacleView.imageDir = imageDir.getSelectedItem().toString();
                obstacleView.invalidate();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {

            }

        });
        xPos.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {
                if(s.length() != 0)
                    obstacleView.x = Integer.parseInt(s.toString());
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                if(s.length() != 0){
                    obstacleView.x = Integer.parseInt(s.toString());
                }


            }
        });
        yPos.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {
                if(s.length() != 0)
                    obstacleView.y = Integer.parseInt(s.toString());
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                if(s.length() != 0){
                    obstacleView.y = Integer.parseInt(s.toString());
                }


            }
        });


        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogDataListener.setObstacleEdit(false);
                getDialog().dismiss();
            }
        });
        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean obstacleEdit = false;
                dialogDataListener.dialogData(getArguments().getInt("OBSINDEX"),imageDir.getSelectedItem().toString(), obstacleView.x, obstacleView.y);
                dialogDataListener.setObstacleEdit(false);
                getDialog().dismiss();
            }
        });


    }
    public interface DialogDataListener {
        void dialogData(int obsIndex, String imageDir, int x, int y);
        void setObstacleEdit(boolean obsEdit);
    }
    DialogDataListener dialogDataListener;
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            dialogDataListener = (DialogDataListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement onSomeEventListener");
        }
    }
    @Override
    public void onPause() {
        super.onPause();
        dismissAllowingStateLoss();
    }
    @Override
    public void onDismiss(DialogInterface frag) {
        super.onDismiss(frag);
        // DO Something
    }


}

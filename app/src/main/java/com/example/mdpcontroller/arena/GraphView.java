package com.example.mdpcontroller.arena;

import android.app.Activity;
import android.content.Context;
import android.provider.ContactsContract;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import com.example.mdpcontroller.MainActivity;
import com.example.mdpcontroller.R;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class GraphView extends com.jjoe64.graphview.GraphView {
    private LineGraphSeries<DataPoint> series1;

    public GraphView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        double x = 0, y;
        series1 = new LineGraphSeries<>();
        int numDataPt = 500;
        for(int i = 0; i <numDataPt;i++){
            x = x+0.1;
            y = Math.sin(x);
            series1.appendData(new DataPoint(x,y), true,100 );
        }
//        GraphView graph = ((MainActivity)getContext()).findViewById(R.id.graphview);


    }
}

package com.example.mdpcontroller.arena;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import com.example.mdpcontroller.R;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.PointsGraphSeries;

import java.util.ArrayList;
import java.util.Map;


public class GraphViewFragment extends Fragment {
    private static final String TAG = "Graph View";
    GraphView graphView;
    private ArrayList<Cell> gridCells ;
    PointsGraphSeries<DataPoint> plots;

    public GraphViewFragment(){}
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_graph_view, container, false);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        gridCells = new ArrayList<>();
        plots = new PointsGraphSeries<>();
        graphView = view.findViewById(R.id.arena_view); //change getActivity() to fragView

        for(double i = 0; i < 20; i++){
            for(double j = 0; j < 20; j++){
                gridCells.add(new Cell(i,j));
            }
        }
        setUpGrids();


    }
    private void setUpGrids(){
        Log.d(TAG,"createScatterPlot: creating scatter plot.");
        for(int i = 0; i < gridCells.size(); i++){
            try{
                double x = gridCells.get(i).getX();
                double y = gridCells.get(i).getY();
                plots.appendData(new DataPoint(x,y),true,450);

            }catch(IllegalArgumentException e){
                Log.e(TAG, "createScatterPlot: IllegalArgumentException: " + e.getMessage());
            }
        }
        graphView.getGridLabelRenderer().setNumHorizontalLabels(20);
        graphView.getGridLabelRenderer().setNumVerticalLabels(20);
        graphView.getViewport().setMaxX(20);
        graphView.getViewport().setMinX(0);
        graphView.getViewport().setMaxY(20);
        graphView.getViewport().setMinY(0);
        graphView.getViewport().setYAxisBoundsManual(true);
        graphView.getViewport().setXAxisBoundsManual(true);

        graphView.getViewport().setScrollable(true);
        graphView.getViewport().setScrollableY(true);
        graphView.getViewport().setScalable(true);
        graphView.getViewport().setScalableY(true);


        plots.setCustomShape(new PointsGraphSeries.CustomShape() {
            @Override
            public void draw(Canvas canvas, Paint paint, float x, float y, DataPointInterface dataPoint) {
                paint.setColor(Color.RED);
                canvas.drawRect(x, y-30, x+30, y, paint);
//                canvas.drawLine(x-10, y-10, x+10, y+10, paint);
//                canvas.drawLine(x+10, y-10, x-10, y+10, paint);
            }
        });
        graphView.addSeries(plots);

    }



}
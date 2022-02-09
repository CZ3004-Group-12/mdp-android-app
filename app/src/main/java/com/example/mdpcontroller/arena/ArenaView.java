package com.example.mdpcontroller.arena;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.mdpcontroller.BluetoothService;
import com.example.mdpcontroller.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Stack;
import java.util.concurrent.TimeUnit;

public class ArenaView extends View {
    //Zoom & Scroll
    //These two constants specify the minimum and maximum zoom
    private static float MIN_ZOOM = 1f;
    private static float MAX_ZOOM = 5f;

    private float scaleFactor = 1.f;
    private ScaleGestureDetector detector;
    private Rect clipBoundsCanvas;

    //These constants specify the mode that we&#039;re in
    private static int NONE = 0;
    private static int DRAG = 1;
    private static int ZOOM = 2;

    private int mode;

    //These two variables keep track of the X and Y coordinate of the finger when it first
    //touches the screen
    private float startX = 0f;
    private float startY = 0f;

    //These two variables keep track of the amount we need to translate the canvas along the X
    //and the Y coordinate
    private float translateX = 0f;
    private float translateY = 0f;

    //These two variables keep track of the amount we translated the X and Y coordinates, the last time we
    //panned.
    private float previousTranslateX = 0f;
    private float previousTranslateY = 0f;

    private boolean dragged;


    //Arena
    private Cell[][] cells;
    private Map<Cell, RectF> gridMap;
    public ArrayList<Obstacle> obstacles;
    private Obstacle movingObs;
    public static final int COLS = 20, ROWS = 20;
    public boolean editMap, isSetRobot, isSetObstacles,obstacleSelected;
    private float cellSize, hMargin, vMargin;
    private final Paint wallPaint,gridPaint,textPaint, robotBodyPaint, robotHeadPaint,obstaclePaint,
            exploredGridPaint, obstacleNumPaint, obstacleImageIDPaint, gridNumberPaint, obstacleHeadPaint,exploredObstaclePaint;


    public ArenaView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        gridMap = new HashMap<Cell, RectF>();
        obstacles = new ArrayList<Obstacle>();
        cells = new Cell[COLS][ROWS];
        clipBoundsCanvas = new Rect();
        createArena();
        Robot.initializeRobot(cells);
        //change accordingly for testing
        editMap = true;
        detector = new ScaleGestureDetector(getContext(), new ScaleListener());

        wallPaint = new Paint();
        wallPaint.setColor(getResources().getColor(R.color.transparent));
        gridPaint = new Paint();
        gridPaint.setColor(getResources().getColor(R.color.gray_700));
        exploredGridPaint = new Paint();
        exploredGridPaint.setColor(getResources().getColor(R.color.gray_500));
        textPaint = new Paint();
        textPaint.setColor(getResources().getColor(R.color.white));
        robotBodyPaint = new Paint();
        robotBodyPaint.setColor(getResources().getColor(R.color.green_500));
        robotHeadPaint = new Paint();
        robotHeadPaint.setColor(getResources().getColor(R.color.green_700));
        obstaclePaint = new Paint();
        obstaclePaint.setColor(getResources().getColor(R.color.black));
        obstacleImageIDPaint = new Paint();
        obstacleImageIDPaint.setColor(getResources().getColor(R.color.white));
        obstacleNumPaint = new Paint();
        obstacleNumPaint.setColor(getResources().getColor(R.color.white));
        obstacleHeadPaint = new Paint();
        obstacleHeadPaint.setColor(getResources().getColor(R.color.red_500));
        exploredObstaclePaint = new Paint();
        exploredObstaclePaint.setColor(getResources().getColor(R.color.purple_500));
        gridNumberPaint = new Paint();
        gridNumberPaint.setColor(getResources().getColor(R.color.white));
    }

    private void createArena(){
        RectF curRect;
        for (int x = 0; x < COLS; x++){
            for (int y = 0; y < ROWS; y++){
                cells[x][y] = new Cell(x,y);
                curRect = new RectF();
                gridMap.put(cells[x][y], curRect);
            }
        }
    }

    //called whenever object of the class is called
    @Override
    protected  void onDraw(Canvas canvas){
        canvas.getClipBounds(clipBoundsCanvas);
        canvas.drawColor(getResources().getColor(R.color.gray_600));
        //size of canvas so that we know how many pixel to work with
        int width = getWidth();
        int height = getHeight();
        if (width/height < COLS/ROWS)
            cellSize = width/(COLS+2);
        else
            cellSize = height/(ROWS+2);
        obstacleImageIDPaint.setTextSize(cellSize/2);
        obstacleNumPaint.setTextSize(cellSize/4);
        gridNumberPaint.setTextSize(cellSize/2);
        hMargin = (width-(COLS+1)*cellSize)/2;
        vMargin = (height-(ROWS+1)*cellSize)/2;
        canvas.translate(hMargin,vMargin);
        //We&#039;re going to scale the X and Y coordinates by the same amount
        canvas.scale(scaleFactor, scaleFactor);

        //If translateX times -1 is lesser than zero, let&#039;s set it to zero. This takes care of the left bound
        if((translateX * -1) < 0) {
            translateX = 0;
        }

        //This is where we take care of the right bound. We compare translateX times -1 to (scaleFactor - 1) * displayWidth.
        //If translateX is greater than that value, then we know that we&#039;ve gone over the bound. So we set the value of
        //translateX to (1 - scaleFactor) times the display width. Notice that the terms are interchanged; it&#039;s the same
        //as doing -1 * (scaleFactor - 1) * displayWidth
        else if((translateX * -1) > (scaleFactor - 1) * width) {
            translateX = (1 - scaleFactor) * width;
        }

        if(translateY * -1 < 0) {
            translateY = 0;
        }

        //We do the exact same thing for the bottom bound, except in this case we use the height of the display
        else if((translateY * -1) > (scaleFactor - 1) * height) {
            translateY = (1 - scaleFactor) * height;
        }

        //We need to divide by the scale factor here, otherwise we end up with excessive panning based on our zoom level
        //because the translation amount also gets scaled according to how much we&#039;ve zoomed into the canvas.
        canvas.translate(translateX / scaleFactor, translateY / scaleFactor);

        //draw grid numbers
        for (int i=0; i<COLS; i++){
            plotSquare(canvas,0,i, wallPaint, gridNumberPaint, String.valueOf(COLS-1-i));
            plotSquare(canvas,i+1,ROWS, wallPaint, gridNumberPaint, String.valueOf(i));
        }

        for (int x = 1; x < COLS+1; x++){ // col 0 is for grid number
            for (int y = 0; y < ROWS; y++){ // row ROWS is for grid number

                // Paint walls
                if(cells[x-1][y].topWall){
                    canvas.drawLine( x* cellSize, y *cellSize, (x+1)* cellSize,y*cellSize, wallPaint);
                }
                if(cells[x-1][y].bottomWall){
                    canvas.drawLine( x* cellSize, (y+1) *cellSize, (x+1)* cellSize,(y+1)*cellSize, wallPaint);
                }
                if(cells[x-1][y].leftWall){
                    canvas.drawLine( x* cellSize, y *cellSize, x* cellSize,(y+1)*cellSize, wallPaint);
                }
                if(cells[x-1][y].rightWall){
                    canvas.drawLine( (x+1)* cellSize, y *cellSize, (x+1)* cellSize,(y+1)*cellSize, wallPaint);
                }

                // Paint normal cell
                RectF cellRect = gridMap.get(cells[x-1][y]);
                if(cellRect != null) {
                    cellRect.set((x + 0.1f) * cellSize, (y + 0.1f) * cellSize, (x + 1f) * cellSize, (y + 1f) * cellSize);

                    int cellRadius = 10;
                    canvas.drawRoundRect(cellRect, // rect
                            cellRadius, // rx
                            cellRadius, // ry
                            gridPaint // Paint
                    );
                }
            }

        }

        // Paint Obstacles
        for(int i = 0; i < obstacles.size(); i++){
            // Default: Paint obstacle with no Image ID
            String txt = String.valueOf(i+1);
            Paint txtPaint = obstacleNumPaint;
            Paint obsPaint = obstaclePaint;
            // Paint obstacle with Image ID
            if (obstacles.get(i).explored) {
                txt = String.valueOf(obstacles.get(i).imageID);
                txtPaint = obstacleImageIDPaint;
                obsPaint = exploredObstaclePaint;
            }
            plotSquare(canvas,(float) obstacles.get(i).cell.col+1,(float) obstacles.get(i).cell.row, obsPaint, txtPaint, txt);
            plotObstacleDir(canvas,obstacles.get(i));
        }

        if (Robot.robotMatrix[0][0] != null){// Skip below if Robot not initialized
            // Paint Robot
            Cell robotCell;
            Paint robotPaint;
            for (int i=0; i<Robot.robotMatrix[0].length; i++){ // iterate through rows: i = x coordinate
                for (int j=0; j<Robot.robotMatrix.length; j++){ // iterate through cols: j = y coordinate
                    robotCell = Robot.robotMatrix[i][j];
                    if(robotCell.type.equals("robotHead")) robotPaint = robotHeadPaint;
                    else robotPaint = robotBodyPaint;
                    plotSquare(canvas,(float) robotCell.col+1,(float) robotCell.row,robotPaint, null, null);
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        if (!isSetObstacles && !isSetRobot){
            scaleGrid(event);
            return true;
        }

        float x = (event.getX()-hMargin)/scaleFactor - translateX / scaleFactor + clipBoundsCanvas.left;
        float y = (event.getY()-vMargin)/scaleFactor - translateY / scaleFactor + clipBoundsCanvas.top;
        Cell curCell;
        RectF curRect;

        for (Map.Entry<Cell, RectF> entry : gridMap.entrySet()) {
            curCell = entry.getKey();
            curRect = entry.getValue();
            if(curRect != null && curCell != null) {
                float rectX = curRect.centerX();
                float rectY = curRect.centerY();
                if (curRect.contains(x , y )) {
                    System.out.println(x + " : " + y + " : " + rectX + " : " + rectY + " : " + hMargin + " : " + vMargin + " : " + cellSize);
                    System.out.println("Coordinates: (" + curCell.col + "," + curCell.row + ")");
                    if(editMap){
                        if(isSetObstacles){
                            if(!obstacleSelected){
                                if(curCell.type == "" && event.getAction()==MotionEvent.ACTION_UP){
                                    curCell.type = "obstacle";
                                    obstacles.add(new Obstacle(curCell));
                                    System.out.println("Obstacles Coordinates: (" + curCell.col + "," + curCell.row + ")");
                                    invalidate();
                                    break;
                                } else if (curCell.type == "obstacle"){
                                    obstacleSelected = true;
                                    for(Obstacle obstacle: obstacles){
                                        if(obstacle.cell == curCell){
                                            movingObs = obstacle;
                                        }
                                    }
                                    System.out.println("Obstacle Selected");
                                }
                            } else if(obstacleSelected && event.getAction()==MotionEvent.ACTION_UP){
                                obstacleSelected = false;
                            } else if(obstacleSelected && event.getAction()==MotionEvent.ACTION_MOVE){
                                dragObstacle(event, entry.getKey(), movingObs);
                            }

                        }else if(isSetRobot){
                            setRobot(curCell.col, curCell.row, "N");
                        }
                    }else{
                        if(curCell.type.equals("obstacle")){
                            System.out.println("POP-UP" + curCell.type);
                            break;
                        }
                    }
                }
            }


        }
        return true;
    }

    private void scaleGrid(MotionEvent event){
        float x = event.getX();
        float y = event.getY();
        Cell curCell;
        RectF curRect;
        for (Map.Entry<Cell, RectF> entry : gridMap.entrySet()) {
            curCell = entry.getKey();
            curRect = entry.getValue();
            if(curRect != null && curCell != null) {
                float rectX = curRect.centerX();
                float rectY = curRect.centerY();
                float translateRectX;
                float translateRectY;

                switch (event.getAction() & MotionEvent.ACTION_MASK) {

                    case MotionEvent.ACTION_DOWN:
                        mode = DRAG;
//                        System.out.println("DOWN");
                        //We assign the current X and Y coordinate of the finger to startX and startY minus the previously translated
                        //amount for each coordinates This works even when we are translating the first time because the initial
                        //values for these two variables is zero.
                        startX = x - previousTranslateX;
                        startY = y - previousTranslateY;
                        break;

                    case MotionEvent.ACTION_MOVE:
                        translateX = x - startX;
                        translateY = y - startY;
                        //We cannot use startX and startY directly because we have adjusted their values using the previous translation values.
                        //This is why we need to add those values to startX and startY so that we can get the actual coordinates of the finger.
                        double distance = Math.sqrt(Math.pow(x - (startX + previousTranslateX), 2) +
                                Math.pow(y - (startY + previousTranslateY), 2)
                        );

                        if(distance > 0) {
                            dragged = true;
                        }
//                            if (curRect.contains(translateX -  (0.25f*cellSize), translateY -cellSize)) {
//                                System.out.println("Coordinates: (" + curCell.row + "," + curCell.col + ")");
//                            }

                        break;

                    case MotionEvent.ACTION_POINTER_DOWN:
                        mode = ZOOM;
//                        System.out.println("POINTER DOWN");
                        break;

                    case MotionEvent.ACTION_UP:
                        mode = NONE;
                        dragged = false;
//                        System.out.println("UP");
                        //All fingers went up, so let&#039;s save the value of translateX and translateY into previousTranslateX and
                        //previousTranslate
                        previousTranslateX = translateX;
                        previousTranslateY = translateY;
//                            System.out.println("Rectangle Coordinates: (" + curRect.centerX() + "," + curRect.centerY() + ")");
//                            System.out.println("Translate Coordinates: (" + translateX + "," + translateY + ")");
                        break;

                    case MotionEvent.ACTION_POINTER_UP:
                        mode = DRAG;

                        //This is not strictly necessary; we save the value of translateX and translateY into previousTranslateX
                        //and previousTranslateY when the second finger goes up
                        previousTranslateX = translateX;
                        previousTranslateY = translateY;
                        break;
                }
            }


        }
//        switch (event.getAction() & MotionEvent.ACTION_MASK) {
//
//            case MotionEvent.ACTION_DOWN:
//                mode = DRAG;
//
//                //We assign the current X and Y coordinate of the finger to startX and startY minus the previously translated
//                //amount for each coordinates This works even when we are translating the first time because the initial
//                //values for these two variables is zero.
//                startX = event.getX() - previousTranslateX;
//                startY = event.getY() - previousTranslateY;
//                break;
//
//            case MotionEvent.ACTION_MOVE:
//                translateX = event.getX() - startX;
//                translateY = event.getY() - startY;
//
//                //We cannot use startX and startY directly because we have adjusted their values using the previous translation values.
//                //This is why we need to add those values to startX and startY so that we can get the actual coordinates of the finger.
//                double distance = Math.sqrt(Math.pow(event.getX() - (startX + previousTranslateX), 2) +
//                        Math.pow(event.getY() - (startY + previousTranslateY), 2)
//                );
//
//                if(distance > 0) {
//                    dragged = true;
//                }
//
//                break;
//
//            case MotionEvent.ACTION_POINTER_DOWN:
//                mode = ZOOM;
//                break;
//
//            case MotionEvent.ACTION_UP:
//                mode = NONE;
//                dragged = false;
//
//                //All fingers went up, so let&#039;s save the value of translateX and translateY into previousTranslateX and
//                //previousTranslate
//                previousTranslateX = translateX;
//                previousTranslateY = translateY;
//                break;
//
//            case MotionEvent.ACTION_POINTER_UP:
//                mode = DRAG;
//
//                //This is not strictly necessary; we save the value of translateX and translateY into previousTranslateX
//                //and previousTranslateY when the second finger goes up
//                previousTranslateX = translateX;
//                previousTranslateY = translateY;
//                break;
//        }

        detector.onTouchEvent(event);

        //We redraw the canvas only in the following cases:
        //
        // o The mode is ZOOM
        //        OR
        // o The mode is DRAG and the scale factor is not equal to 1 (meaning we have zoomed) and dragged is
        //   set to true (meaning the finger has actually moved)
        if ((mode == DRAG && scaleFactor != 1f && dragged) || mode == ZOOM) {
            invalidate();
        }
    }

    private void dragObstacle(MotionEvent event, Cell curCell, Obstacle obstacle){
        if(curCell.type.equals("")){
            switch (event.getAction()) {
                case MotionEvent.ACTION_UP:
                    curCell.type = "obstacle";
                    obstacle.cell.col = curCell.col;
                    obstacle.cell.row = curCell.row;
                    obstacleSelected = false;
                    break;
                default:
                    obstacleSelected = true;
                    curCell.type = "";
                    obstacle.cell.col = curCell.col;
                    obstacle.cell.row = curCell.row;
                    break;
            }
        }

        invalidate();
    }

    private void plotSquare(Canvas canvas, float x, float y, Paint paint, Paint numPaint, String text){
        RectF cellRect = new RectF(
                (x+0.1f)*cellSize,
                (y+0.1f)*cellSize,
                (x+1f)*cellSize,
                (y+1f)*cellSize);
        int cellRadius = 10;
        canvas.drawRoundRect(cellRect, // rect
                cellRadius, // rx
                cellRadius, // ry
                paint // Paint
        );
        // draw number on obstacle
        if (text != null){
            float cellWidth = ((x+1f)*cellSize - (x+0.1f)*cellSize);
            float cellHeight = ((y+1f)*cellSize - (y+0.1f)*cellSize);
            Rect bounds = new Rect();
            numPaint.getTextBounds(text, 0, text.length(), bounds);
            canvas.drawText(text,
                    ((x+0.1f)*cellSize + cellWidth / 2f - bounds.width() / 2f - bounds.left),
                    ((y+0.1f)*cellSize + cellHeight / 2f + bounds.height() / 2f - bounds.bottom),
                    numPaint);
        }
    }
    private void plotObstacleDir(Canvas canvas,Obstacle obstacle){
        RectF cellRect = new RectF(0,0,0,0);
        // For all plotting of rectangles, need to +1 to the col value to account for the grid numbers
        switch (obstacle.imageDir) {
            case "TOP":
                cellRect = new RectF((obstacle.cell.col + 0.2f + 1) * cellSize, (obstacle.cell.row + 0.11f) * cellSize, (obstacle.cell.col + 0.9f + 1) * cellSize, (obstacle.cell.row + (1f / 4)) * cellSize);
                break;
            case "LEFT":
                cellRect = new RectF((obstacle.cell.col + 0.11f + 1) * cellSize, (obstacle.cell.row + 0.2f) * cellSize, (obstacle.cell.col + (1f / 4) + 1) * cellSize, (obstacle.cell.row + 0.9f) * cellSize);
                break;
            case "RIGHT":
                cellRect = new RectF((obstacle.cell.col + (1f / 4) + 1) * cellSize, (obstacle.cell.row + 0.2f) * cellSize, (obstacle.cell.col + 1f + 1) * cellSize, (obstacle.cell.row + 0.9f) * cellSize);
                break;
            case "BOTTOM":
                cellRect = new RectF((obstacle.cell.col + 0.2f + 1) * cellSize, (obstacle.cell.row + (1f / 4)) * cellSize, (obstacle.cell.col + 0.9f + 1) * cellSize, (obstacle.cell.row + 0.11f) * cellSize);
                break;
        }
        int cellRadius = 1000;
        canvas.drawRoundRect(cellRect, // rect
                cellRadius, // rx
                cellRadius, // ry
                obstacleHeadPaint // Paint
        );
    }


    public void setObstacleImageID(String obstacleNumber, String imageID){
        if (Integer.parseInt(obstacleNumber)-1 < obstacles.size()) {
            Obstacle obs = obstacles.get(Integer.parseInt(obstacleNumber)-1);
            obs.imageID = imageID;
            obs.explored = true;
            invalidate();
        }
    }

    public void setRobot(int xCenter, int yCenter,  String dir){
        if(yCenter<1 || yCenter>=ROWS-1 || xCenter>=COLS-1 || xCenter<1){
            System.out.println("Out of bound : Robot need six cells");
        }else{
            Robot.setRobot(xCenter, yCenter, dir);
            invalidate();
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(MIN_ZOOM, Math.min(scaleFactor, MAX_ZOOM));
            return true;
        }
    }

    public void clearObstacles(){
        obstacles.clear();
        invalidate();
    }
}

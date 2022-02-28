package com.example.mdpcontroller.arena;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.annotation.Nullable;
import com.example.mdpcontroller.R;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class ArenaView extends View {
    //Zoom & Scroll
    private static float MIN_ZOOM = 1f;
    private static float MAX_ZOOM = 5f;
    private float scaleFactor = 1.f;
    private ScaleGestureDetector detector;
    private Rect clipBoundsCanvas;
    private static int NONE = 0;
    private static int DRAG = 1;
    private static int ZOOM = 2;
    private int mode;
    private float startX = 0f;
    private float startY = 0f;
    private float translateX = 0f;
    private float translateY = 0f;
    private float previousTranslateX = 0f;
    private float previousTranslateY = 0f;
    private boolean dragged;


    //Arena
    private Cell[][] cells;
    public Map<Cell, RectF> gridMap;
    public ArrayList<Obstacle> obstacles;
    public Obstacle editingObs;
    public static final int COLS = 20, ROWS = 20;
    public boolean isEditMap, isSetRobot, isSetObstacles,obstacleSelected, obstacleEdit;
    private float cellSize, hMargin, vMargin;
    private final Paint wallPaint,gridPaint,textPaint, robotBodyPaint, robotHeadPaint,obstaclePaint,
            exploredGridPaint, obstacleNumPaint, obstacleImageIDPaint, gridNumberPaint, obstacleHeadPaint,exploredObstaclePaint;
    Cell maxRight,maxLeft;




    public ArenaView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        gridMap = new HashMap<Cell, RectF>();
        obstacles = new ArrayList<Obstacle>();
        cells = new Cell[COLS][ROWS];
        clipBoundsCanvas = new Rect();
        createArena();
        Robot.initializeRobot(cells);
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
        exploredObstaclePaint.setColor(getResources().getColor(R.color.purple_200));
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
        int width = getWidth();
        int height = getHeight();
        if (width/height < COLS/ROWS)
            cellSize = width/(COLS+2);
        else
            cellSize = height/(ROWS+2);
        obstacleImageIDPaint.setTextSize(cellSize/2);
        obstacleNumPaint.setTextSize(cellSize/3);
        gridNumberPaint.setTextSize(cellSize/2);
        hMargin = (width-(COLS+1)*cellSize)/2;
        vMargin = (height-(ROWS+1)*cellSize)/2;
        canvas.translate(hMargin,vMargin);
        canvas.scale(scaleFactor, scaleFactor);

        if((translateX * -1) < 0) {
            translateX = 0;
        }
        else if((translateX * -1) > (scaleFactor - 1) * width) {
            translateX = (1 - scaleFactor) * width;
        }
        if(translateY * -1 < 0) {
            translateY = 0;
        }
        else if((translateY * -1) > (scaleFactor - 1) * height) {
            translateY = (1 - scaleFactor) * height;
        }
        canvas.translate(translateX / scaleFactor, translateY / scaleFactor);

        //draw grid numbers
        for (int i=0; i<COLS; i++){
            plotSquare(canvas,0,i, wallPaint, gridNumberPaint, String.valueOf(COLS-1-i));
            plotSquare(canvas,i+1,ROWS, wallPaint, gridNumberPaint, String.valueOf(i));
        }

        for (int x = 1; x < COLS+1; x++){ // col 0 is for grid number
            for (int y = 0; y < ROWS; y++){ // row ROWS is for grid number

                // Paint normal cell
                RectF cellRect = gridMap.get(cells[x-1][y]);
                if(cellRect != null) {
                    cellRect.set((x + 0.1f) * cellSize, (y + 0.1f) * cellSize, (x + 1f) * cellSize, (y + 1f) * cellSize);
                    int cellRadius = 10;
                    canvas.drawRoundRect(cellRect, cellRadius, cellRadius, gridPaint);
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
        if (!isSetObstacles && !isSetRobot && !obstacleEdit){
            scaleGrid(event);
            return true;
        }
        if(isSetObstacles || isSetRobot){
            isEditMap = true;
        }

        float x = (event.getX()-hMargin)/scaleFactor - translateX / scaleFactor + clipBoundsCanvas.left;
        float y = (event.getY()-vMargin)/scaleFactor - translateY / scaleFactor + clipBoundsCanvas.top;
        Cell curCell;
        RectF curRect;

        for (Map.Entry<Cell, RectF> entry : gridMap.entrySet()) {
            curCell = entry.getKey();
            curRect = entry.getValue();
            if(curCell.col== 0 && curCell.row == 0){
                maxLeft = entry.getKey();
            }else if(curCell.col== 19 && curCell.row == 19){
                maxRight = entry.getKey();
            }
            if(curRect != null && curCell != null) {
                float rectX = curRect.centerX();
                float rectY = curRect.centerY();
                if (curRect.contains(x , y )) {
                    System.out.println(x + " : " + y + " : " + rectX + " : " + rectY + " : " + hMargin + " : " + vMargin + " : " + cellSize);
                    System.out.println("Coordinates: (" + curCell.col + "," + curCell.row + ")");
                    if(isEditMap){
                        if(isSetObstacles){
                            if(!obstacleSelected){
                                if(curCell.type == "" && event.getAction()==MotionEvent.ACTION_UP){
                                    curCell.type = "obstacle";
                                    obstacles.add(new Obstacle(curCell));
                                    System.out.println("Obstacles Coordinates: (" + curCell.col + "," + curCell.row + ")");
                                    invalidate();
                                    break;
                                } else if (curCell.type == "obstacle"){
                                    for(Obstacle obstacle: obstacles){
                                        if(obstacle.cell == curCell){
                                            editingObs = obstacle;
                                        }
                                    }
                                    obstacleSelected = true;
                                    System.out.println("Obstacle Selected");
                                }
                            } else if(obstacleSelected && event.getAction()==MotionEvent.ACTION_UP){
                                if(curCell.col == editingObs.cell.col && curCell.row == editingObs.cell.row && curCell.type==""){
                                    curCell.type = "obstacle";
                                }
                                invalidate();
                                obstacleSelected = false;
                            } else if(obstacleSelected && event.getAction()==MotionEvent.ACTION_MOVE){
                                dragObstacle(event, entry.getKey(), editingObs);

                            }

                        }else if(isSetRobot){
                            setRobot(curCell.col, curCell.row, "N");
                        }
                    }
                    else{
                        obstacleEdit = false;
                    }
                } else if(obstacleSelected && editingObs != null && isSetObstacles){
                    //Obstacle delete
                    if(event.getAction() ==MotionEvent.ACTION_MOVE){
                        if(x < gridMap.get(maxLeft).centerX() || x > gridMap.get(maxRight).centerX()){
                            if(editingObs != null){
                                obstacles.remove(editingObs);
                                obstacleSelected = false;
                                invalidate();
                            }

                        }
                    }
                }
            }


        }
        return true;
    }

    private void scaleGrid(MotionEvent event){
        isEditMap = false;
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
                        startX = x - previousTranslateX;
                        startY = y - previousTranslateY;
                        break;

                    case MotionEvent.ACTION_MOVE:
                        translateX = x - startX;
                        translateY = y - startY;
                        double distance = Math.sqrt(Math.pow(x - (startX + previousTranslateX), 2) +
                                Math.pow(y - (startY + previousTranslateY), 2)
                        );

                        if(distance > 0) {
                            dragged = true;
                        }
                        break;

                    case MotionEvent.ACTION_POINTER_DOWN:
                        mode = ZOOM;
                        break;

                    case MotionEvent.ACTION_UP:
                        mode = NONE;
                        dragged = false;
                        previousTranslateX = translateX;
                        previousTranslateY = translateY;
                        setObstacleEdit(event,curCell,curRect);
                        break;

//                    case MotionEvent.ACTION_POINTER_UP:
//                        mode = DRAG;
//                        previousTranslateX = translateX;
//                        previousTranslateY = translateY;
//                        break;
                }
            }
        }
        detector.onTouchEvent(event);

        if ((mode == DRAG && scaleFactor != 1f && dragged) || mode == ZOOM) {
            invalidate();
        }
    }

    private void dragObstacle(MotionEvent event, Cell curCell, Obstacle obstacle){
        int index = obstacles.indexOf(obstacle);
        try{
            if(curCell.type.equals("")){
                switch (event.getAction()) {
                    case MotionEvent.ACTION_UP:
                        if(obstacles.size() != 0){
                            curCell.setType("obstacle");
                            obstacles.get(index).cell = curCell;
                            obstacleSelected = false;
                        }
                        break;
                    default:
                        if(obstacles.size() != 0){
                            obstacleSelected = true;
                            curCell.type = "";
                            obstacles.get(index).cell = curCell;
                        }

                        break;
                }
            }
        }catch (Exception e){
            System.out.print(e);
        }

        invalidate();
    }
    private void setObstacleEdit(MotionEvent event, Cell curCell, RectF curRect){
        float x = (event.getX()-hMargin)/scaleFactor - translateX / scaleFactor + clipBoundsCanvas.left;
        float y = (event.getY()-vMargin)/scaleFactor - translateY / scaleFactor + clipBoundsCanvas.top;
        if (curRect.contains(x , y )) {
            for(Obstacle obstacle: obstacles){
                if(obstacle.cell == curCell){
                    obstacleEdit = true;
                    editingObs = obstacle;
                    if (dataEventListener != null) {
                        dataEventListener.onEventOccurred();
                    }
                }
            }
        }

    }

    private void plotSquare(Canvas canvas, float x, float y, Paint paint, Paint numPaint, String text){
        RectF cellRect = new RectF(
                (x+0.1f)*cellSize,
                (y+0.1f)*cellSize,
                (x+1f)*cellSize,
                (y+1f)*cellSize);
        int cellRadius = 10;
        canvas.drawRoundRect(cellRect, // rectf
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
                cellRect = new RectF((obstacle.cell.col+1.2f) * cellSize, (obstacle.cell.row +0.1f) * cellSize, (obstacle.cell.col +1.9f) * cellSize, (obstacle.cell.row +0.25f) * cellSize);
                break;
            case "LEFT":
                cellRect = new RectF((obstacle.cell.col +1.1f) * cellSize, (obstacle.cell.row +0.2f) * cellSize, (obstacle.cell.col + 1.25f) * cellSize, (obstacle.cell.row + 0.9f) * cellSize);
                break;
            case "RIGHT":
                cellRect = new RectF((obstacle.cell.col+ 1.85f) * cellSize, (obstacle.cell.row + 0.2f) * cellSize, (obstacle.cell.col + 2f) * cellSize, (obstacle.cell.row + 0.9f) * cellSize);
                break;
            case "BOTTOM":
                cellRect = new RectF((obstacle.cell.col+1.2f) * cellSize, (obstacle.cell.row+ 0.85f)* cellSize, (obstacle.cell.col + 1.9f) * cellSize, (obstacle.cell.row + 1f)*cellSize);
                //(1f), canvasHeight, 1f*canvasWidth, (1f/1.25f)*canvasHeight
                break;
        }
        int cellRadius = 10;
        canvas.drawRoundRect(cellRect, cellRadius, cellRadius, obstacleHeadPaint);
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
            Robot.setRobot(xCenter, yCenter, dir,obstacles);
            invalidate();
        }
    }
    public String moveRobot(String dir,String movement){
        Robot.moveRobot(dir,movement,obstacles);
        invalidate();
        return Robot.robotDir;
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


    //To listen arena view data changes on Main activity.
    public interface DataEventListener {
        public void onEventOccurred();
    }

    private DataEventListener dataEventListener;

    public void setEventListener(DataEventListener dataEventListener) {
        this.dataEventListener = dataEventListener;
    }
}

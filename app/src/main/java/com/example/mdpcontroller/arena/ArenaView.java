package com.example.mdpcontroller.arena;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
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

public class ArenaView extends View {
    //Arena
    private Cell[][] cells;
    private Map<Cell, RectF> gridMap;
    Robot robot = new Robot();
    private ArrayList<Obstacle> obstacles;
    private ArrayList<Cell> robotCells;
    private enum Direction{ //data type of self defined constant
        UP, DOWN, LEFT, RIGHT
    }
    private Cell player, exit;
    private static final int COLS = 12, ROWS = 13;
    private static final float WALL_THICKNESS = 4;
    public boolean editMap, isSetRobot, isSetObstacles;
    //cell size, horizontal margin and verticl margin
    private float cellSize, hMargin, vMargin;
    private Paint wallPaint,gridPaint,textPaint, robotBodyPaint, robotHeadPaint,obstaclePaint,exploredGridPaint, obstacleNumPaint, obstacleImageIDPaint;

    //For random generator to pick unvisited neighbour
    private Random random;
    private BluetoothService btService;


    public ArenaView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        gridMap = new HashMap<Cell, RectF>();
        obstacles = new ArrayList<Obstacle>();
        robotCells = new ArrayList<Cell>();
        //change accordingly for testing
        editMap = true;

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


        createArena();
    }

    private void createArena(){
        cells = new Cell[COLS][ROWS];
        for (int x = 0; x < COLS; x++){
            for (int y = 0; y < ROWS; y++){
                cells[x][y] = new Cell(x,y);
            }
        }
    }

    //called whenever object of the class is called
    @Override
    protected  void onDraw(Canvas canvas){
        canvas.drawColor(getResources().getColor(R.color.gray_600));
        //size of canvas so that we know how many pixel to work with
        int width = getWidth();
        int height = getHeight();
        if (width/height < COLS/ROWS)
            cellSize = width/(COLS+1);
        else
            cellSize = height/(ROWS+1);
        obstacleImageIDPaint.setTextSize(cellSize/2);
        obstacleNumPaint.setTextSize(cellSize/4);
        hMargin = (width-COLS*cellSize)/2;
        vMargin = (height-ROWS*cellSize)/2;
        canvas.translate(hMargin,vMargin);
        for (int x = 0; x < COLS; x++){
            for (int y = 0; y < ROWS; y++){
                if(cells[x][y].topWall){
                    canvas.drawLine( x* cellSize, y *cellSize, (x+1)* cellSize,y*cellSize, wallPaint);
                }
                if(cells[x][y].bottomWall){
                    canvas.drawLine( x* cellSize, (y+1) *cellSize, (x+1)* cellSize,(y+1)*cellSize, wallPaint);
                }
                if(cells[x][y].leftWall){
                    canvas.drawLine( x* cellSize, y *cellSize, x* cellSize,(y+1)*cellSize, wallPaint);
                }
                if(cells[x][y].rightWall){
                    canvas.drawLine( (x+1)* cellSize, y *cellSize, (x+1)* cellSize,(y+1)*cellSize, wallPaint);
                }
                RectF cellRect = new RectF((x+0.1f) * cellSize,(y+0.1f) *cellSize,(x+1f)* cellSize,(y+1f)* cellSize);
                int cellRadius = 10;
                canvas.drawRoundRect(cellRect, // rect
                        cellRadius, // rx
                        cellRadius, // ry
                        gridPaint // Paint
                );
                gridMap.put(cells[x][y], cellRect);
                for(int i = 0; i < obstacles.size(); i++){
                    String txt = String.valueOf(i+1);
                    Paint txtPaint = obstacleNumPaint;
                    if (!obstacles.get(i).imageID.equals("-1")) {
                        txt = String.valueOf(obstacles.get(i).imageID);
                        txtPaint = obstacleImageIDPaint;
                    }
                    plotSquare(canvas,(float) obstacles.get(i).cell.col,(float) obstacles.get(i).cell.row, obstaclePaint, txtPaint, txt);
                }
                for(int i = 0; i < robotCells.size(); i++){
                    if(robotCells.get(i).type == "robotHead"){
                        plotSquare(canvas,(float) robotCells.get(i).col,(float) robotCells.get(i).row,robotHeadPaint, null, null);
                    }else{
                        plotSquare(canvas,(float) robotCells.get(i).col,(float) robotCells.get(i).row,robotBodyPaint, null, null);
                    }
                }
            }
        }

    }
    @Override
    public boolean onTouchEvent(MotionEvent event){
        float x = event.getX();
        float y = event.getY();
        for (Map.Entry<Cell, RectF> entry : gridMap.entrySet()) {
            if(entry.getValue() == null)
                break;
            else {
                float rectX = entry.getValue().centerX();
                float rectY = entry.getValue().centerY();
                if (entry.getValue().contains(x -  (0.25f*cellSize), y -cellSize)) {
                    System.out.println(x + " : " + y + " : " + rectX + " : " + rectY + " : " + hMargin + " : " + vMargin + " : " + cellSize);
                    System.out.println("Coordinates: (" + entry.getKey().row + "," + entry.getKey().col + ")");
                    if(editMap){
                        if(isSetObstacles){
                            if(entry.getKey().type == ""){
//                                if (event.getAction() == MotionEvent.ACTION_MOVE)
//                                    return false;
                                Cell tempKey = new Cell(entry.getKey().col, entry.getKey().row, "obstacle");
                                gridMap.put(tempKey,entry.getValue());
                                System.out.println(gridMap.get(entry.getKey()));
                                System.out.println(gridMap.get(tempKey));
                                obstacles.add(new Obstacle(entry.getKey(), false));
                                System.out.println("Obstacles Coordinates: (" + entry.getKey().row + "," + entry.getKey().col + ")");
                                //btService.write(String.format(Locale.getDefault(),"CREATE/%02d/%02d/%02d", gridMap.size(), entry.getKey().row, entry.getKey().col));
                                gridMap.remove(entry.getKey());
                                invalidate();
                                break;
                            }
                            else if (entry.getKey().type == "obstacle"){
                                // for dragging still doing
                            }else{
                                System.out.println("Grid is occupied");
                            }
                        }else if(isSetRobot){
                            if(entry.getKey().row-1 == -1 || entry.getKey().row+1 == ROWS || entry.getKey().col+1 == COLS || entry.getKey().col-1 == -1){
                                System.out.println("Out of bound : Robot need six cells");
                            }else{
                                if(entry.getKey() != null){
                                    robotCells = robot.getRobotCells(entry.getKey());
                                    for(int i = 0; i < robotCells.size();i++){
                                        setRobot(robotCells.get(i),robotCells.get(i).type,gridMap.get(new Cell(robotCells.get(i).col,robotCells.get(i).row)));
                                    }
                                    invalidate();
                                    break;
                                }
                            }

                        }
                    }else{
                        if(entry.getKey().type == "obstacle"){
                            System.out.println("POP-UP" + entry.getKey().type);
                            break;
                        }
                    }
                }
            }


        }
        return super.onTouchEvent(event);
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
    private void moveObstacle(Direction direction, Obstacle obstacle){
        //resetting the grid type
        Cell tempKey = new Cell(obstacle.cell.col, obstacle.cell.row, "");
        gridMap.put(tempKey,gridMap.get(obstacle.cell));
        System.out.println(gridMap.get(obstacle.cell));
        System.out.println(gridMap.get(tempKey));
        //distinguish four cases with switch statement
        switch(direction){
            case UP:
                obstacle.cell = new Cell(obstacle.cell.col, obstacle.cell.row-1);
                gridMap.remove(obstacle.cell);
                break;
            case DOWN:
                obstacle.cell = new Cell(obstacle.cell.col, obstacle.cell.row+1);
                gridMap.remove(obstacle.cell);
                break;
            case LEFT:
                obstacle.cell = new Cell(obstacle.cell.col-1, obstacle.cell.row);
                gridMap.remove(obstacle.cell);
                break;
            case RIGHT:
                obstacle.cell = new Cell(obstacle.cell.col+1, obstacle.cell.row);
                gridMap.remove(obstacle.cell);
        }
    }

    private void setRobot(Cell oldCell, String type, RectF rect){
        Cell tempKey;
        if(type == "robotHead"){
            tempKey = new Cell(oldCell.col, oldCell.row, "robotHead");
        }else{
            tempKey = new Cell(oldCell.col, oldCell.row, "robot");
        }
        gridMap.put(tempKey,rect);
        gridMap.remove(oldCell);
    }

    public void setObstacleImageID(String obstacleNumber, String imageID){
        if (Integer.parseInt(obstacleNumber)-1 < obstacles.size()) {
            obstacles.get(Integer.parseInt(obstacleNumber)-1).setImageID(imageID);
            invalidate();
        }
    }

    public void setBtService(BluetoothService btService){
        this.btService = btService;
    }
}

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
    private ArrayList<Obstacle> obstacles;
    private ArrayList<Cell> robotCells;
    private enum Direction{ //data type of self defined constant
        UP, DOWN, LEFT, RIGHT
    }
    private Cell player, exit;
    private static final int COLS = 20, ROWS = 20;
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
        cells = new Cell[COLS][ROWS];
        createArena();
        Robot.initializeRobot(cells);
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
    }

    private void createArena(){
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

                // Paint walls
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

                // Paint normal cell
                RectF cellRect = new RectF((x+0.1f) * cellSize,(y+0.1f) *cellSize,(x+1f)* cellSize,(y+1f)* cellSize);
                int cellRadius = 10;
                canvas.drawRoundRect(cellRect, // rect
                        cellRadius, // rx
                        cellRadius, // ry
                        gridPaint // Paint
                );
                gridMap.put(cells[x][y], cellRect);

            }
        }

        // Paint Obstacles
        for(int i = 0; i < obstacles.size(); i++){
            // Default: Paint obstacle with no Image ID
            String txt = String.valueOf(i+1);
            Paint txtPaint = obstacleNumPaint;
            // Paint obstacle with Image ID
            if (!obstacles.get(i).imageID.equals("-1")) {
                txt = String.valueOf(obstacles.get(i).imageID);
                txtPaint = obstacleImageIDPaint;
            }
            plotSquare(canvas,(float) obstacles.get(i).cell.col,(float) obstacles.get(i).cell.row, obstaclePaint, txtPaint, txt);
        }

        if (Robot.robotMatrix[0][0] == null) return; // Skip below if Robot not initialized

        // Paint Robot
        Cell robotCell;
        Paint robotPaint;
        for (int i=0; i<Robot.robotMatrix[0].length; i++){ // iterate through rows: i = x coordinate
            for (int j=0; j<Robot.robotMatrix.length; j++){ // iterate through cols: j = y coordinate
                robotCell = Robot.robotMatrix[i][j];
                if(robotCell.type.equals("robotHead")) robotPaint = robotHeadPaint;
                else robotPaint = robotBodyPaint;
                plotSquare(canvas,(float) robotCell.col,(float) robotCell.row,robotPaint, null, null);
            }
        }

    }
    @Override
    public boolean onTouchEvent(MotionEvent event){
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
                if (curRect.contains(x -  (0.25f*cellSize), y -cellSize)) {
                    System.out.println(x + " : " + y + " : " + rectX + " : " + rectY + " : " + hMargin + " : " + vMargin + " : " + cellSize);
                    System.out.println("Coordinates: (" + curCell.row + "," + curCell.col + ")");
                    if(editMap){
                        if(isSetObstacles){
                            if(curCell.type == ""){
//                                if (event.getAction() == MotionEvent.ACTION_MOVE)
//                                    return false;
                                Cell tempKey = new Cell(curCell.col, curCell.row, "obstacle");
                                gridMap.put(tempKey,curRect);
                                System.out.println(gridMap.get(curCell));
                                System.out.println(gridMap.get(tempKey));
                                obstacles.add(new Obstacle(curCell, false));
                                System.out.println("Obstacles Coordinates: (" + curCell.row + "," + curCell.col + ")");
                                //btService.write(String.format(Locale.getDefault(),"CREATE/%02d/%02d/%02d", gridMap.size(), curCell.row, curCell.col));
                                gridMap.remove(curCell);
                                invalidate();
                                break;
                            }
                            else if (curCell.type == "obstacle"){
                                // for dragging still doing
                            }else{
                                System.out.println("Grid is occupied");
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

    public void setObstacleImageID(String obstacleNumber, String imageID){
        if (Integer.parseInt(obstacleNumber)-1 < obstacles.size()) {
            obstacles.get(Integer.parseInt(obstacleNumber)-1).setImageID(imageID);
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

    public void setBtService(BluetoothService btService){
        this.btService = btService;
    }
}

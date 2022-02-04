package com.example.mdpcontroller.arena;

import android.graphics.Canvas;
import android.graphics.RectF;

import java.util.ArrayList;
import java.util.Map;

/**
 * I look like this
 * [R, H, R]
 * [R, R, R]
 * [R, R, R]
 * Where each element in the array is a Cell, and letter corresponds to the Cell type
 */
public class Robot {
    public static Cell[][] robotMatrix;
    //  Looks like this:
    //  [B, H, B]
    //  [B, B, B]
    //  [B, B, B]

    public static void initializeRobot(){
        robotMatrix = new Cell[3][3];
    }
    public static void setRobot(Cell centre,  String dir, Cell[][] cells){
        setRobotPosition(centre, cells);
        setRobotDirection(dir);
    }
    /**
     * Does NOT validate if position is valid, Robot should not know about the state of the grid
     * Perform validation before calling this method
     * @param centre new centre of the robot
     */
    private static void setRobotPosition(Cell centre, Cell[][] cells){
        int yTopLeft=centre.row-1, xTopLeft= centre.col-1;
        Cell curCell;
        // wipe old robot position
        if (robotMatrix[0][0] != null){ // skip on initial robot set
            for (int i=0; i<robotMatrix[0].length; i++){ // iterate through rows: i = x coordinate
                for (int j=0; j<robotMatrix.length; j++){ // iterate through cols: j = y coordinate
                    robotMatrix[i][j].type = "";
                }
            }
        }

        // set new robot position
        for (int i=0; i<robotMatrix[0].length; i++){ // iterate through rows: i = x coordinate
            for (int j=0; j<robotMatrix.length; j++){ // iterate through cols: j = y coordinate
                curCell = cells[xTopLeft+i][yTopLeft+j];
                curCell.type = "robot";
                robotMatrix[i][j] = curCell;
            }
        }
    }

    /**
     * Sets the direction of the Robot
     * @param dir Possible values: N, S, E, W
     */
    private static void setRobotDirection(String dir){
        // get head
        int xHead =1 , yHead = 0; // Default is N
        switch(dir){
            case("N"): {
                xHead = 1; yHead = 0; break;
            }
            case("S"): {
                xHead = 1; yHead = 2; break;
            }
            case("E"): {
                xHead = 2; yHead = 1; break;
            }
            case("W"): {
                xHead = 0; yHead = 1; break;
            }
        }

        // Update robot direction
        for (int i=0; i<robotMatrix[0].length; i++){ // iterate through cols: i = x coordinate
            for (int j=0; j<robotMatrix.length; j++){ // iterate through cols: j = y coordinate
                if (i==xHead && j==yHead) robotMatrix[i][j].type = "robotHead";
                else robotMatrix[i][j].type = "robot"; // reset old head
            }
        }
    }

}

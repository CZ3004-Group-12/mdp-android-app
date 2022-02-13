package com.example.mdpcontroller.arena;

import java.util.ArrayList;

/**
 * I look like this
 * [R, H, R]
 * [R, R, R]
 * [R, R, R]
 * Where each element in the array is a Cell, and letter corresponds to the Cell type
 */
public class Robot {
    public static Cell[][] robotMatrix;
    private static Cell[][] grid;
    //  Looks like this:
    //  [B, H, B]
    //  [B, B, B]
    //  [B, B, B]

    public static void initializeRobot(Cell[][] cells){
        robotMatrix = new Cell[3][3];
        robotMatrix[1][1] = null;
        grid = cells;
    }
    public static void setRobot(int xCenter, int yCenter,  String dir, ArrayList<Obstacle> obstacles){
        boolean newPos = checkObs(xCenter,yCenter,obstacles);
        if(!newPos){
            setRobotPosition(xCenter, yCenter);
            setRobotDirection(dir);
        }
    }
    /**
     * Does NOT validate if position is valid, Robot should not know about the state of the grid
     * Perform validation before calling this method
     * @param xCenter x Coordinate of new centre of robot
     * @param yCenter y Coordinate of new centre of robot
     */
        private static void setRobotPosition(int xCenter, int yCenter){
        int yTopLeft=yCenter-1, xTopLeft= xCenter-1;
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
                curCell = grid[xTopLeft+i][yTopLeft+j];
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

    /**
     * Check whether at least one of the new robot cells is obstacle
     * @param xCenter x Coordinate of new centre of robot
     * @param yCenter y Coordinate of new centre of robot
     * @param obstacles list of plotted obstacles
     */
    private static boolean checkObs(int xCenter,int yCenter,ArrayList<Obstacle> obstacles){
        int yTopLeft=yCenter-1, xTopLeft= xCenter-1;
        for (int i=0; i<robotMatrix[0].length; i++){ // iterate through rows: i = x coordinate
            for (int j=0; j<robotMatrix.length; j++){ // iterate through cols: j = y coordinate
                for(Obstacle obs: obstacles ){
                    if((obs.cell.col == xTopLeft+i) &&(obs.cell.row == yTopLeft+j)){
                        return true;
                    }
                }
            }
        }
        return false;
    }


}

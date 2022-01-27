package com.example.mdpcontroller.arena;

import android.graphics.Canvas;

import java.util.ArrayList;

public class Robot {
    private ArrayList<ArrayList<Cell>> robotMatrix;
    private Cell robotHead;

    public ArrayList<Cell> getRobotCells(Cell cell){
        ArrayList<Cell> robotCells= new ArrayList<Cell>();
        robotCells.add(new Cell(cell.col ,cell.row-1,"robotHead"));
        robotCells.add(new Cell(cell.col+1 ,cell.row-1,"robot"));
        robotCells.add(new Cell(cell.col-1 ,cell.row-1,"robot"));
        robotCells.add(new Cell(cell.col ,cell.row,"robot"));
        robotCells.add(new Cell(cell.col+1 ,cell.row,"robot"));
        robotCells.add(new Cell(cell.col-1 ,cell.row,"robot"));
        robotCells.add(new Cell(cell.col ,cell.row+1,"robot"));
        robotCells.add(new Cell(cell.col+1 ,cell.row+1,"robot"));
        robotCells.add(new Cell(cell.col-1 ,cell.row+1,"robot"));

        return robotCells;
    }

}

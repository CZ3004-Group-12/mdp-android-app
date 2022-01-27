package com.example.mdpcontroller.arena;

public class Cell {
    boolean
            topWall = true,
            leftWall = true,
            bottomWall = true,
            rightWall = true,
            visited = false;
    String type = "";
    int col, row;
    public Cell (int col, int row){
        this.col = col;
        this.row = row;
    }
    public Cell (int col, int row, String type){
        this.col = col;
        this.row = row;
        this.type = type;
    }

}

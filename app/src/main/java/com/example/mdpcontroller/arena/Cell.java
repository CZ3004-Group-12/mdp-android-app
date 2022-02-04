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

    @Override
    public boolean equals(Object o){
        if (o instanceof Cell){
            Cell that = (Cell) o;
            return (this.col==that.col) && (this.row== that.row);
        }
        return false;
    }
}

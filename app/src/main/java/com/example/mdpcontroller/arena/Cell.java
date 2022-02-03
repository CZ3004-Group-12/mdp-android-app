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
    double x, y;
    public Cell (int col, int row){
        this.col = col;
        this.row = row;
    }

    public Cell (int col, int row, String type){
        this.col = col;
        this.row = row;
        this.type = type;
    }
    public Cell (double x, double y){
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }
}

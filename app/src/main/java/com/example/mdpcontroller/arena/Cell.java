package com.example.mdpcontroller.arena;

public class Cell {
    String type = "";
    public int col, row;
    public Cell (int col, int row){
        this.col = col;
        this.row = row;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getCol() {
        return col;
    }

    public void setCol(int col) {
        this.col = col;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
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

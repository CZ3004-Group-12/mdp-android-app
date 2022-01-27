package com.example.mdpcontroller.arena;

public class Obstacle {
    Cell cell;
    int imageID;
    boolean explored = false;
    String imagePos = "";

    public Obstacle (Cell cell, boolean explored){
        this.cell = cell;
        this.cell.type = "obstacle";
        this.explored = explored;
    }
    public Obstacle (Cell cell, boolean explored, int imageID, String imagePos){
        this.cell = cell;
        this.explored = explored;
        this.imageID = imageID;
        this.imagePos = imagePos;
    }
}

package com.example.mdpcontroller.arena;

public class Obstacle {
    Cell cell;
    String imageID;
    boolean explored = false;
    String imagePos = "";

    public Obstacle (Cell cell, boolean explored){
        this.cell = cell;
        this.cell.type = "obstacle";
        this.explored = explored;
        this.imageID = "-1";
    }
    public Obstacle (Cell cell, boolean explored, String imagePos){
        this.cell = cell;
        this.explored = explored;
        this.imagePos = imagePos;
    }

    public void setImageID(String imageID){
        this.imageID = imageID;
    }
}

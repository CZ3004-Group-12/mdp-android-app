package com.example.mdpcontroller.arena;

public class Obstacle {
    Cell cell;
    String imageID;
    boolean explored = true;
    String imageDir = "";

    public Obstacle (Cell cell, boolean explored){
        this.cell = cell;
        this.cell.type = "obstacle";
        this.explored = explored;
        this.imageID = "-1";
        this.imageDir = "TOP";
    }
    public Obstacle (Cell cell, boolean explored, String dir){
        this.cell = cell;
        this.explored = explored;
        this.imageDir = imageDir;
    }

    public void setImageID(String imageID){
        this.imageID = imageID;
    }
}

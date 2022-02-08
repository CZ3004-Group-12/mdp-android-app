package com.example.mdpcontroller.arena;

public class Obstacle {
    public Cell cell;
    public String imageID;
    public boolean explored;
    public String imageDir = "";

    public Obstacle (Cell cell){
        this.cell = cell;
        this.cell.type = "obstacle";
        this.explored = false;
        this.imageID = "-1";
        this.imageDir = "TOP";
    }
}

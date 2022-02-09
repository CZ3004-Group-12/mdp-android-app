package com.example.mdpcontroller.arena;

import android.os.Parcel;
import android.os.Parcelable;

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
    public Obstacle (Cell cell, boolean explored, String dir){
        this.cell = cell;
        this.explored = explored;
        this.imageDir = imageDir;
    }

    public Cell getCell() {
        return cell;
    }

    public void setCell(Cell cell) {
        this.cell = cell;
    }

    public String getImageID() {
        return imageID;
    }

    public boolean isExplored() {
        return explored;
    }

    public void setExplored(boolean explored) {
        this.explored = explored;
    }

    public String getImageDir() {
        return imageDir;
    }

    public void setImageDir(String imageDir) {
        this.imageDir = imageDir;
    }

    public void setImageID(String imageID){
        this.imageID = imageID;
    }
}

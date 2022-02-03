package com.example.mdpcontroller.tab;

import android.content.ClipData;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class AppStateViewModel extends ViewModel {
    private final MutableLiveData<Boolean> isSetRobot = new MutableLiveData<Boolean>();
    private final MutableLiveData<Boolean> isRobotMove = new MutableLiveData<Boolean>();
    private final MutableLiveData<Boolean> isSetObstacles = new MutableLiveData<Boolean>();
    private final MutableLiveData<Boolean> isRobotStop = new MutableLiveData<Boolean>();
    private final MutableLiveData<Boolean> isReset = new MutableLiveData<Boolean>();
    public void setRobot(Boolean item) {
        isSetRobot.setValue(item);
    }
    public LiveData<Boolean> getRobot() {
        return isSetRobot;
    }
    public void setObstacles(Boolean item) {
        isSetObstacles.setValue(item);
    }
    public LiveData<Boolean> getObstacles() {
        return isSetObstacles;
    }
    public void setRobotMove(Boolean item) {
        isRobotMove.setValue(item);
    }
    public LiveData<Boolean> getRobotMove() {
        return isRobotMove;
    }
    public void setRobotStop(Boolean item) {
        isRobotStop.setValue(item);
    }
    public LiveData<Boolean> getRobotStop() {
        return isRobotStop;
    }
    public void setIsReset(Boolean item) {
        isReset.setValue(item);
    }
    public LiveData<Boolean> getIsReset() {
        return isReset;
    }

}

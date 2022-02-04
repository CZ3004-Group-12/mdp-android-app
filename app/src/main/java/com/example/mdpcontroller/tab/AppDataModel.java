package com.example.mdpcontroller.tab;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class AppDataModel extends ViewModel {
    private final MutableLiveData<Boolean> isSetRobot = new MutableLiveData<Boolean>();
    private final MutableLiveData<Boolean> isRobotMove = new MutableLiveData<Boolean>();
    private final MutableLiveData<Boolean> isSetObstacles = new MutableLiveData<Boolean>();
    private final MutableLiveData<Boolean> isRobotStop = new MutableLiveData<Boolean>();
    private final MutableLiveData<Boolean> isReset = new MutableLiveData<Boolean>();
    private final MutableLiveData<String> robotDirection = new MutableLiveData<String>();

    public void setIsSetRobot(Boolean item) {
        isSetRobot.setValue(item);
    }
    public LiveData<Boolean> getIsSetRobot() {
        return isSetRobot;
    }
    public void setIsSetObstacles(Boolean item) {
        isSetObstacles.setValue(item);
    }
    public LiveData<Boolean> getIsSetObstacles() {
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
    public void setRobotDirection(String item) {
        robotDirection.setValue(item);
    }
    public LiveData<String> getRobotDirection() {
        return robotDirection;
    }
}

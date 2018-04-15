package com.example.overlord.btech_project.model;

/**
 * Created by overlord on 15/4/18.
 */
public class Fused {
    public Double heartBeat;
    public float accelerationX;
    public float accelerationY;
    public float accelerationZ;

    public Fused() {}

    public Fused(Double beats, float values[]) {

        heartBeat     = beats;
        accelerationX = values[0];
        accelerationY = values[1];
        accelerationZ = values[2];
    }

    public Double getHeartBeat() {
        return heartBeat;
    }

    public void setHeartBeat(Double heartBeat) {
        this.heartBeat = heartBeat;
    }

    public float getAccelerationX() {
        return accelerationX;
    }

    public void setAccelerationX(float accelerationX) {
        this.accelerationX = accelerationX;
    }

    public float getAccelerationY() {
        return accelerationY;
    }

    public void setAccelerationY(float accelerationY) {
        this.accelerationY = accelerationY;
    }

    public float getAccelerationZ() {
        return accelerationZ;
    }

    public void setAccelerationZ(float accelerationZ) {
        this.accelerationZ = accelerationZ;
    }

    @Override
    public String toString() {
        return "" + heartBeat + ", (" + accelerationX + ", " + accelerationY + ", " + accelerationZ + ")";
    }
}

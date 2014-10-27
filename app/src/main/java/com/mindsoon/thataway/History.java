package com.mindsoon.thataway;

public class History {
    private final double latitude;
    private final double longitude;
    private final int time;

    public History(double latitude, double longitude, int time) {
        this.latitude=latitude;
        this.longitude=longitude;
        this.time=time;
    }

    public double getLatitude(){
        return this.latitude;
    }

    public double getLongitude(){
        return this.longitude;
    }

    public int getTime(){
        return this.time;
    }

}
package com.lab.multiplexer.tomtom.Activity.Model;

/**
 * Created by Majid on 6/24/2017.
 */

public class NearBiker  extends Biker{
    double distance;
    Biker biker;

    public NearBiker(double distance, Biker biker) {
        this.distance = distance;
        this.biker = biker;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public Biker getBiker() {
        return biker;
    }

    public void setBiker(Biker biker) {
        this.biker = biker;
    }
}

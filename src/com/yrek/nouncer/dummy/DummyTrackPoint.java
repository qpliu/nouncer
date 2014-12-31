package com.yrek.nouncer.dummy;

import com.yrek.nouncer.data.Location;
import com.yrek.nouncer.data.TrackPoint;

class DummyTrackPoint implements TrackPoint {
    private final DummyLocation location;
    private final long entryTime;
    private final double entryHeading;
    private final double entrySpeed;
    private long exitTime;
    private double exitHeading;
    private double exitSpeed;

    DummyTrackPoint(DummyLocation location, long entryTime, double entryHeading, double entrySpeed) {
        this.location = location;
        this.entryTime = entryTime;
        this.entryHeading = entryHeading;
        this.entrySpeed = entrySpeed;
        this.exitTime = entryTime;
        this.exitHeading = entryHeading;
        this.exitSpeed = entrySpeed;
    }

    @Override
    public Location getLocation() {
        return location;
    }

    @Override
    public long getEntryTime() {
        return entryTime;
    }

    @Override
    public long getExitTime() {
        return exitTime;
    }

    @Override
    public double getEntryHeading() {
        return entryHeading;
    }

    @Override
    public double getExitHeading() {
        return exitHeading;
    }

    @Override
    public double getEntrySpeed() {
        return entrySpeed;
    }

    @Override
    public double getExitSpeed() {
        return exitSpeed;
    }

    void setExitTime(long exitTime, double exitHeading, double exitSpeed) {
        this.exitTime = exitTime;
        this.exitHeading = exitHeading;
        this.exitSpeed = exitSpeed;
    }
}

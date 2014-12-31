package com.yrek.nouncer.data;

public interface TrackPoint {
    public Location getLocation();
    public long getEntryTime();
    public long getExitTime();
    public double getEntryHeading();
    public double getExitHeading();
    public double getEntrySpeed();
    public double getExitSpeed();
}

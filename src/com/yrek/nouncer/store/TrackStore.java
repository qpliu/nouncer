package com.yrek.nouncer.store;

import java.util.List;

import com.yrek.nouncer.data.Location;
import com.yrek.nouncer.data.TrackPoint;

public interface TrackStore {
    public List<TrackPoint> getTrackPoints(long minTimestamp, long maxTimestamp, int maxPoints);
    public boolean addEntry(Location location, long entryTime, double entryHeading, double entrySpeed, long timestamp);
    public boolean addExit(Location location, long exitTime, double exitHeading, double exitSpeed, long timestamp);
    public void deleteOlderThan(long timestamp);
}

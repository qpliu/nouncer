package com.yrek.nouncer.store;

import java.util.List;

import com.yrek.nouncer.data.Location;
import com.yrek.nouncer.data.TrackPoint;

public interface TrackStore {
    public List<TrackPoint> getTrackPoints(long minTimestamp, long maxTimestamp);
    public void addEntry(Location location, long timestamp);
    public void addExit(Location location, long timestamp);
}

package com.yrek.nouncer.dummy;

import com.yrek.nouncer.data.Location;
import com.yrek.nouncer.data.TrackPoint;

class DummyTrackPoint implements TrackPoint {
    private final DummyLocation location;
    private final long entryTime;
    private long exitTime;

    DummyTrackPoint(DummyLocation location, long entryTime) {
        this.location = location;
        this.entryTime = entryTime;
        this.exitTime = entryTime;
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

    void setExitTime(long exitTime) {
        this.exitTime = exitTime;
    }
}

package com.yrek.nouncer.dummy;

import java.util.ArrayList;
import java.util.List;

import com.yrek.nouncer.data.Location;
import com.yrek.nouncer.data.TrackPoint;
import com.yrek.nouncer.store.TrackStore;

class DummyTrackStore implements TrackStore {
    @Override
    public List<TrackPoint> getTrackPoints(long minTimestamp, long maxTimestamp) {
        ArrayList<TrackPoint> list = new ArrayList<TrackPoint>();
        return list;
    }

    @Override
    public void addEntry(Location location, long timestamp) {
    }

    @Override
    public void addExit(Location location, long timestamp) {
    }
}

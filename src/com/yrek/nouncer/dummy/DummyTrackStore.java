package com.yrek.nouncer.dummy;

import java.util.ArrayList;
import java.util.List;

import com.yrek.nouncer.data.Location;
import com.yrek.nouncer.data.TrackPoint;
import com.yrek.nouncer.store.TrackStore;

class DummyTrackStore implements TrackStore {
    final ArrayList<DummyTrackPoint> track = new ArrayList<DummyTrackPoint>();

    @Override
    public List<TrackPoint> getTrackPoints(long minTimestamp, long maxTimestamp, int maxPoints) {
        ArrayList<TrackPoint> list = new ArrayList<TrackPoint>();
        int i = track.size() - 1;
        synchronized (track) {
            while (i >= 0 && track.get(i).getEntryTime() > maxTimestamp) {
                i--;
            }
            while (i >= 0 && (maxPoints <= 0 || list.size() < maxPoints)) {
                DummyTrackPoint point = track.get(i);
                if (point.getExitTime() < minTimestamp) {
                    break;
                }
                list.add(point);
                i--;
            }
        }
        return list;
    }

    @Override
    public boolean addEntry(Location location, long timestamp) {
        synchronized (track) {
            if (!track.isEmpty()) {
                if (track.get(track.size() - 1).getExitTime() >= timestamp) {
                    return false;
                }
            }
            track.add(new DummyTrackPoint((DummyLocation) location, timestamp));
        }
        return true;
    }

    @Override
    public boolean addExit(Location location, long timestamp) {
        synchronized (track) {
            if (!track.isEmpty()) {
                DummyTrackPoint last = track.get(track.size() - 1);
                if (last.getExitTime() > timestamp) {
                    return false;
                } else if (last.getLocation().equals(location)) {
                    last.setExitTime(timestamp);
                    return true;
                }
            }
            track.add(new DummyTrackPoint((DummyLocation) location, timestamp));
        }
        return true;
    }
}

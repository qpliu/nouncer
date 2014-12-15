package com.yrek.nouncer.processor;

import com.yrek.nouncer.data.Location;
import com.yrek.nouncer.data.Point;

import com.yrek.nouncer.store.LocationStore;

public class PointProcessor {
    private final LocationStore locationStore;
    private final Listener listener;

    public PointProcessor(LocationStore locationStore, Listener listener) {
        this.locationStore = locationStore;
        this.listener = listener;
    }

    public void receivePoint(Point point) {
    }

    public interface Listener {
        public void receiveEntry(Location location, long timestamp);
        public void receiveExit(Location location, long timestamp);
    }
}

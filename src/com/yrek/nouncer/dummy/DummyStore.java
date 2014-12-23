package com.yrek.nouncer.dummy;

import com.yrek.nouncer.store.LocationStore;
import com.yrek.nouncer.store.PointStore;
import com.yrek.nouncer.store.RouteStore;
import com.yrek.nouncer.store.Store;
import com.yrek.nouncer.store.TrackStore;

public class DummyStore implements Store {
    private final DummyLocationStore locationStore = new DummyLocationStore();
    private final DummyRouteStore routeStore = new DummyRouteStore();
    private final DummyPointStore pointStore = new DummyPointStore();
    private final DummyTrackStore trackStore = new DummyTrackStore();

    @Override
    public LocationStore getLocationStore() {
        return locationStore;
    }

    @Override
    public RouteStore getRouteStore() {
        return routeStore;
    }

    @Override
    public PointStore getPointStore() {
        return pointStore;
    }

    @Override
    public TrackStore getTrackStore() {
        return trackStore;
    }
}

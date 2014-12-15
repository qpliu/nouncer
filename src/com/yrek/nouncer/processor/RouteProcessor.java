package com.yrek.nouncer.processor;

import com.yrek.nouncer.data.Location;
import com.yrek.nouncer.data.Route;
import com.yrek.nouncer.data.RoutePoint;
import com.yrek.nouncer.store.RouteStore;

public class RouteProcessor implements PointProcessor.Listener {
    private final RouteStore routeStore;
    private final Listener listener;

    public RouteProcessor(RouteStore routeStore, Listener listener) {
        this.routeStore = routeStore;
        this.listener = listener;
    }

    @Override
    public void receiveEntry(Location location, long timestamp) {
    }

    @Override
    public void receiveExit(Location location, long timestamp) {
    }

    public interface Listener {
        public void receiveEntry(Route route, int routeIndex, long timestamp);
        public void receiveExit(Route route, int routeIndex, long timestamp);
    }
}

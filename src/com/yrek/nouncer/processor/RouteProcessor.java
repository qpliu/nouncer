package com.yrek.nouncer.processor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.yrek.nouncer.data.Location;
import com.yrek.nouncer.data.Route;
import com.yrek.nouncer.data.TrackPoint;
import com.yrek.nouncer.store.RouteStore;
import com.yrek.nouncer.store.TrackStore;

public class RouteProcessor implements PointProcessor.Listener {
    private final RouteStore routeStore;
    private final TrackStore trackStore;
    private final Listener listener;

    public RouteProcessor(RouteStore routeStore, TrackStore trackStore, Listener listener) {
        this.routeStore = routeStore;
        this.trackStore = trackStore;
        this.listener = listener;
    }

    private void process(Location location, long timestamp, boolean entry) {
        Collection<Route> routes = routeStore.getRoutes(location);
        int length = 0;
        for (Route route : routes) {
            length = Math.max(length, route.getRoutePointCount());
        }
        if (length <= 0) {
            return;
        }
        ArrayList<PendingRoute> pendingRoutes = new ArrayList<PendingRoute>();
        List<TrackPoint> trackPoints = trackStore.getTrackPoints(0, timestamp, length);
        Collections.reverse(trackPoints);
        for (TrackPoint trackPoint : trackPoints) {
            for (Iterator<PendingRoute> i = pendingRoutes.iterator(); i.hasNext(); ) {
                PendingRoute p = i.next();
                if (p.index + 1 >= p.route.getRoutePointCount() || !p.route.getRoutePoint(p.index + 1).getLocation().equals(trackPoint.getLocation())) {
                    i.remove();
                    continue;
                }
                p.index++;
            }
            for (Route route : routes) {
                if (route.getRoutePoint(0).getLocation().equals(trackPoint.getLocation())) {
                    pendingRoutes.add(new PendingRoute(route, trackPoint.getExitTime()));
                }
            }
        }
        for (PendingRoute p : pendingRoutes) {
            assert p.route.getRoutePoint(p.index).getLocation().equals(location);
            if (entry) {
                listener.receiveEntry(p.route, p.startTime, p.index, timestamp);
            } else {
                listener.receiveExit(p.route, p.startTime, p.index, timestamp);
            }
        }
    }

    @Override
    public void receiveEntry(Location location, long timestamp) {
        process(location, timestamp, true);
    }

    @Override
    public void receiveExit(Location location, long timestamp) {
        process(location, timestamp, false);
    }

    public interface Listener {
        public void receiveEntry(Route route, long startTime, int routeIndex, long timestamp);
        public void receiveExit(Route route, long startTime, int routeIndex, long timestamp);
    }

    private class PendingRoute {
        final Route route;
        final long startTime;
        int index;

        PendingRoute(Route route, long startTime) {
            this.route = route;
            this.startTime = startTime;
            this.index = 0;
        }
    }
}

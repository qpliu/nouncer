package com.yrek.nouncer.processor;

import java.util.ArrayList;
import java.util.Iterator;

import com.yrek.nouncer.data.Location;
import com.yrek.nouncer.data.Route;
import com.yrek.nouncer.data.RoutePoint;
import com.yrek.nouncer.store.RouteStore;

public class RouteProcessor implements PointProcessor.Listener {
    private final RouteStore routeStore;
    private final Listener listener;
    private final ArrayList<PendingRoute> pendingRoutes = new ArrayList<PendingRoute>();

    public RouteProcessor(RouteStore routeStore, Listener listener) {
        this.routeStore = routeStore;
        this.listener = listener;
    }

    @Override
    public void receiveEntry(Location location, long timestamp) {
        synchronized (pendingRoutes) {
            for (Iterator<PendingRoute> i = pendingRoutes.iterator(); i.hasNext(); ) {
                PendingRoute p = i.next();
                if (p.index + 1 >= p.route.getRoutePointCount() || !p.exited || !p.route.getRoutePoint(p.index + 1).getLocation().equals(location)) {
                    i.remove();
                    continue;
                }
                p.index++;
                p.exited = false;
                listener.receiveEntry(p.route, p.startExitTime, p.index, timestamp);
            }
            for (Route r : routeStore.getRoutesStartingAt(location)) {
                pendingRoutes.add(new PendingRoute(r, timestamp));
                listener.receiveEntry(r, timestamp, 0, timestamp);
            }
        }
    }

    @Override
    public void receiveExit(Location location, long timestamp) {
        synchronized (pendingRoutes) {
            for (Iterator<PendingRoute> i = pendingRoutes.iterator(); i.hasNext(); ) {
                PendingRoute p = i.next();
                if (p.index >= p.route.getRoutePointCount() || p.exited || !p.route.getRoutePoint(p.index).getLocation().equals(location)) {
                    i.remove();
                    continue;
                }
                p.exited = true;
                if (p.index == 0) {
                    p.startExitTime = timestamp;
                }
                listener.receiveExit(p.route, p.startExitTime, p.index, timestamp);
                
            }
            for (Route r : routeStore.getRoutesStartingAt(location)) {
                pendingRoutes.add(new PendingRoute(r, timestamp));
                listener.receiveExit(r, timestamp, 0, timestamp);
            }
        }
    }

    public interface Listener {
        public void receiveEntry(Route route, long startTime, int routeIndex, long timestamp);
        public void receiveExit(Route route, long startTime, int routeIndex, long timestamp);
    }

    private class PendingRoute {
        final Route route;
        final long startEntryTime;
        long startExitTime;
        int index;
        boolean exited;

        PendingRoute(Route route, long startEntryTime) {
            this.route = route;
            this.startEntryTime = startEntryTime;
            this.startExitTime = startEntryTime;
            this.index = 0;
            this.exited = false;
        }
    }
}

package com.yrek.nouncer.store;

import java.util.Collection;

import com.yrek.nouncer.data.Location;
import com.yrek.nouncer.data.Route;

public interface RouteStore {
    public Collection<Route> getRoutes();
    public Collection<Route> getRoutes(Location location);
    public Collection<Route> getRoutesStartingAt(Location location);
}

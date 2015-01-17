package com.yrek.nouncer.store;

import com.yrek.nouncer.data.Location;
import com.yrek.nouncer.data.Route;
import com.yrek.nouncer.external.LinkStore;

public interface Store {
    public LocationStore getLocationStore();
    public RouteStore getRouteStore();
    public PointStore getPointStore();
    public TrackStore getTrackStore();
    public AvailabilityStore getAvailabilityStore();
    public LinkStore<Location> getLocationLinkStore();
    public LinkStore<Route> getRouteLinkStore();
}

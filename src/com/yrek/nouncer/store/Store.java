package com.yrek.nouncer.store;

public interface Store {
    public LocationStore getLocationStore();
    public RouteStore getRouteStore();
    public PointStore getPointStore();
    public TrackStore getTrackStore();
}

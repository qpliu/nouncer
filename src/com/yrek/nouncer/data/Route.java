package com.yrek.nouncer.data;

import java.util.List;

public interface Route {
    public String getName();
    public int getRoutePointCount();
    public RoutePoint getRoutePoint(int index);
    public List<RoutePoint> getRoutePoints();
    public boolean isStarred();
    public void setStarred(boolean starred);
}

package com.yrek.nouncer.dummy;

import java.util.ArrayList;
import java.util.List;

import com.yrek.nouncer.data.Route;
import com.yrek.nouncer.data.RoutePoint;

class DummyRoute implements Route {
    private final String name;
    private final DummyRoutePoint[] routePoints;

    DummyRoute(String name, DummyRoutePoint... routePoints) {
        this.name = name;
        this.routePoints = routePoints;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getRoutePointCount() {
        return routePoints.length;
    }

    @Override
    public RoutePoint getRoutePoint(int index) {
        return routePoints[index];
    }

    @Override
    public List<RoutePoint> getRoutePoints() {
        ArrayList<RoutePoint> list = new ArrayList<RoutePoint>();
        for (DummyRoutePoint routePoint : routePoints) {
            list.add(routePoint);
        }
        return list;
    }
}


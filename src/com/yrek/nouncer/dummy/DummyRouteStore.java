package com.yrek.nouncer.dummy;

import java.util.ArrayList;
import java.util.Collection;

import com.yrek.nouncer.data.Location;
import com.yrek.nouncer.data.Route;
import com.yrek.nouncer.store.RouteStore;

class DummyRouteStore implements RouteStore {
    @Override
    public Collection<Route> getRoutes(Location location) {
        ArrayList<Route> routes = new ArrayList<Route>();
        if (location.equals(DummyLocationStore.UCSC_LOOP_1) || location.equals(DummyLocationStore.UCSC_LOOP_2) || location.equals(DummyLocationStore.UCSC_LOOP_3)) {
            routes.add(UCSC_LOOP);
        } else if (location.equals(DummyLocationStore.BONNY_DOON_1) || location.equals(DummyLocationStore.BONNY_DOON_2) || location.equals(DummyLocationStore.BONNY_DOON_3) || location.equals(DummyLocationStore.BONNY_DOON_4) || location.equals(DummyLocationStore.BONNY_DOON_5) || location.equals(DummyLocationStore.BONNY_DOON_6)) {
            routes.add(BONNY_DOON);
        } else if (location.equals(DummyLocationStore.FELTON_EMPIRE_1) || location.equals(DummyLocationStore.FELTON_EMPIRE_2)) {
            routes.add(FELTON_EMPIRE);
        } else if (location.equals(DummyLocationStore.JAMISON_CREEK_1) || location.equals(DummyLocationStore.JAMISON_CREEK_2)) {
            routes.add(JAMISON_CREEK);
        } else if (location.equals(DummyLocationStore.ALBA_1) || location.equals(DummyLocationStore.ALBA_2)) {
            routes.add(ALBA);
        } else if (location.equals(DummyLocationStore.DOWNTOWN_1) || location.equals(DummyLocationStore.DOWNTOWN_2)) {
            routes.add(DOWNTOWN_1);
            routes.add(DOWNTOWN_2);
        }
        return routes;
    }

    @Override
    public Collection<Route> getRoutesStartingAt(Location location) {
        ArrayList<Route> routes = new ArrayList<Route>();
        if (location.equals(DummyLocationStore.UCSC_LOOP_1)) {
            routes.add(UCSC_LOOP);
        } else if (location.equals(DummyLocationStore.BONNY_DOON_1)) {
            routes.add(BONNY_DOON);
        } else if (location.equals(DummyLocationStore.FELTON_EMPIRE_1)) {
            routes.add(FELTON_EMPIRE);
        } else if (location.equals(DummyLocationStore.JAMISON_CREEK_1)) {
            routes.add(JAMISON_CREEK);
        } else if (location.equals(DummyLocationStore.ALBA_1)) {
            routes.add(ALBA);
        } else if (location.equals(DummyLocationStore.DOWNTOWN_1)) {
            routes.add(DOWNTOWN_1);
        } else if (location.equals(DummyLocationStore.DOWNTOWN_2)) {
            routes.add(DOWNTOWN_2);
        }
        return routes;
    }

    private static final String ANNOUNCE_START = "%1$tl:%1$tM";
    private static final String ANNOUNCE_ARRIVE = "%2$d:%3$02d";

    static final DummyRoute UCSC_LOOP =
        new DummyRoute("UCSC Loop",
                       new DummyRoutePoint(DummyLocationStore.UCSC_LOOP_1, null, ANNOUNCE_START),
                       new DummyRoutePoint(DummyLocationStore.UCSC_LOOP_2, ANNOUNCE_ARRIVE, null),
                       new DummyRoutePoint(DummyLocationStore.UCSC_LOOP_3, ANNOUNCE_ARRIVE, null),
                       new DummyRoutePoint(DummyLocationStore.UCSC_LOOP_1, ANNOUNCE_ARRIVE, null));
    static final DummyRoute BONNY_DOON =
        new DummyRoute("Bonny Doon/Pine Flat",
                       new DummyRoutePoint(DummyLocationStore.BONNY_DOON_1, null, ANNOUNCE_START),
                       new DummyRoutePoint(DummyLocationStore.BONNY_DOON_2, ANNOUNCE_ARRIVE, null),
                       new DummyRoutePoint(DummyLocationStore.BONNY_DOON_3, ANNOUNCE_ARRIVE, null),
                       new DummyRoutePoint(DummyLocationStore.BONNY_DOON_4, ANNOUNCE_ARRIVE, null),
                       new DummyRoutePoint(DummyLocationStore.BONNY_DOON_5, ANNOUNCE_ARRIVE, null),
                       new DummyRoutePoint(DummyLocationStore.BONNY_DOON_6, ANNOUNCE_ARRIVE, null));
    static final DummyRoute FELTON_EMPIRE =
        new DummyRoute("Felton-Empire",
                       new DummyRoutePoint(DummyLocationStore.FELTON_EMPIRE_1, null, ANNOUNCE_START),
                       new DummyRoutePoint(DummyLocationStore.FELTON_EMPIRE_2, ANNOUNCE_ARRIVE, null));
    static final DummyRoute JAMISON_CREEK =
        new DummyRoute("Jamison Creek",
                       new DummyRoutePoint(DummyLocationStore.JAMISON_CREEK_1, null, ANNOUNCE_START),
                       new DummyRoutePoint(DummyLocationStore.JAMISON_CREEK_2, ANNOUNCE_ARRIVE, null));
    static final DummyRoute ALBA =
        new DummyRoute("Alba",
                       new DummyRoutePoint(DummyLocationStore.ALBA_1, null, ANNOUNCE_START),
                       new DummyRoutePoint(DummyLocationStore.ALBA_2, ANNOUNCE_ARRIVE, null));
    static final DummyRoute DOWNTOWN_1 =
        new DummyRoute("Downtown 1",
                       new DummyRoutePoint(DummyLocationStore.DOWNTOWN_1, null, ANNOUNCE_START),
                       new DummyRoutePoint(DummyLocationStore.DOWNTOWN_2, ANNOUNCE_ARRIVE, null));
    static final DummyRoute DOWNTOWN_2 =
        new DummyRoute("Downtown 2",
                       new DummyRoutePoint(DummyLocationStore.DOWNTOWN_2, null, ANNOUNCE_START),
                       new DummyRoutePoint(DummyLocationStore.DOWNTOWN_1, ANNOUNCE_ARRIVE, null));
}

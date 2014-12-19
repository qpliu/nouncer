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
        } else if (location.equals(DummyLocationStore.WESTSIDE_1)) {
            routes.add(WESTSIDE_1);
        } else if (location.equals(DummyLocationStore.WESTSIDE_2)) {
            routes.add(WESTSIDE_2);
        } else if (location.equals(DummyLocationStore.WESTSIDE_3)) {
            routes.add(WESTSIDE_3);
        } else if (location.equals(DummyLocationStore.WESTSIDE_4)) {
            routes.add(WESTSIDE_4);
        } else if (location.equals(DummyLocationStore.WESTSIDE_5)) {
            routes.add(WESTSIDE_5);
        } else if (location.equals(DummyLocationStore.WESTSIDE_6)) {
            routes.add(WESTSIDE_6);
        } else if (location.equals(DummyLocationStore.WESTSIDE_7)) {
            routes.add(WESTSIDE_7);
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
        } else if (location.equals(DummyLocationStore.WESTSIDE_1)) {
            routes.add(WESTSIDE_1);
        } else if (location.equals(DummyLocationStore.WESTSIDE_2)) {
            routes.add(WESTSIDE_2);
        } else if (location.equals(DummyLocationStore.WESTSIDE_3)) {
            routes.add(WESTSIDE_3);
        } else if (location.equals(DummyLocationStore.WESTSIDE_4)) {
            routes.add(WESTSIDE_4);
        } else if (location.equals(DummyLocationStore.WESTSIDE_5)) {
            routes.add(WESTSIDE_5);
        } else if (location.equals(DummyLocationStore.WESTSIDE_6)) {
            routes.add(WESTSIDE_6);
        } else if (location.equals(DummyLocationStore.WESTSIDE_7)) {
            routes.add(WESTSIDE_7);
        }
        return routes;
    }

    private static final String ANNOUNCE_START = "%1$tl:%1$tM";
    private static final String ANNOUNCE_ARRIVE = "%2$d minutes %3$02d seconds %2$d minutes %3$02d seconds %2$d minutes %3$02d seconds";
    private static final String ANNOUNCE_ENTER = "Enter %1$tl:%1$tM";
    private static final String ANNOUNCE_EXIT = "Exit %1$tl:%1$tM";

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
                       new DummyRoutePoint(DummyLocationStore.DOWNTOWN_1, ANNOUNCE_ENTER, ANNOUNCE_START),
                       new DummyRoutePoint(DummyLocationStore.DOWNTOWN_2, ANNOUNCE_ARRIVE, ANNOUNCE_EXIT));
    static final DummyRoute DOWNTOWN_2 =
        new DummyRoute("Downtown 2",
                       new DummyRoutePoint(DummyLocationStore.DOWNTOWN_2, ANNOUNCE_ENTER, ANNOUNCE_START),
                       new DummyRoutePoint(DummyLocationStore.DOWNTOWN_1, ANNOUNCE_ARRIVE, ANNOUNCE_EXIT));
    static final DummyRoute WESTSIDE_1 = new DummyRoute("Westside 1", new DummyRoutePoint(DummyLocationStore.WESTSIDE_1, ANNOUNCE_ENTER, ANNOUNCE_EXIT));
    static final DummyRoute WESTSIDE_2 = new DummyRoute("Westside 2", new DummyRoutePoint(DummyLocationStore.WESTSIDE_2, ANNOUNCE_ENTER, ANNOUNCE_EXIT));
    static final DummyRoute WESTSIDE_3 = new DummyRoute("Westside 3", new DummyRoutePoint(DummyLocationStore.WESTSIDE_3, ANNOUNCE_ENTER, ANNOUNCE_EXIT));
    static final DummyRoute WESTSIDE_4 = new DummyRoute("Westside 4", new DummyRoutePoint(DummyLocationStore.WESTSIDE_4, ANNOUNCE_ENTER, ANNOUNCE_EXIT));
    static final DummyRoute WESTSIDE_5 = new DummyRoute("Westside 5", new DummyRoutePoint(DummyLocationStore.WESTSIDE_5, ANNOUNCE_ENTER, ANNOUNCE_EXIT));
    static final DummyRoute WESTSIDE_6 = new DummyRoute("Westside 6", new DummyRoutePoint(DummyLocationStore.WESTSIDE_6, ANNOUNCE_ENTER, ANNOUNCE_EXIT));
    static final DummyRoute WESTSIDE_7 = new DummyRoute("Westside 7", new DummyRoutePoint(DummyLocationStore.WESTSIDE_7, ANNOUNCE_ENTER, ANNOUNCE_EXIT));
}

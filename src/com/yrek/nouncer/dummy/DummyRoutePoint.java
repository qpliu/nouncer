package com.yrek.nouncer.dummy;

import com.yrek.nouncer.data.Location;
import com.yrek.nouncer.data.RoutePoint;

class DummyRoutePoint implements RoutePoint {
    private final DummyLocation location;
    private final String entryAnnouncement;
    private final String exitAnnouncement;

    DummyRoutePoint(DummyLocation location, String entryAnnouncement, String exitAnnouncement) {
        this.location = location;
        this.entryAnnouncement = entryAnnouncement;
        this.exitAnnouncement = exitAnnouncement;
    }

    @Override
    public Location getLocation() {
        return location;
    }

    @Override
    public String getEntryAnnouncement() {
        return entryAnnouncement;
    }

    @Override
    public String getExitAnnouncement() {
        return exitAnnouncement;
    }
}

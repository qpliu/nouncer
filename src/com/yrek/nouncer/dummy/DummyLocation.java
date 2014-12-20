package com.yrek.nouncer.dummy;

import com.yrek.nouncer.data.Location;

class DummyLocation implements Location {
    private final String name;
    private final double latitude;
    private final double longitude;
    private final double elevation;

    DummyLocation(String name, double latitude, double longitude, double elevation) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.elevation = elevation;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public double getLatitude() {
        return this.latitude;
    }

    @Override
    public double getLongitude() {
        return this.longitude;
    }

    @Override
    public double getElevation() {
        return this.elevation;
    }

    @Override
    public boolean equals(Object object) {
        return object == this;
    }
}

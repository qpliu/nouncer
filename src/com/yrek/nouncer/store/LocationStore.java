package com.yrek.nouncer.store;

import java.util.Collection;

import com.yrek.nouncer.data.Location;

public interface LocationStore {
    public Collection<Location> getLocations(double latitude, double longitude, double radius);
}

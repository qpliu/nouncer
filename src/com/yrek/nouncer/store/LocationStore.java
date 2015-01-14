package com.yrek.nouncer.store;

import java.util.Collection;

import com.yrek.nouncer.data.Location;

public interface LocationStore {
    public Collection<Location> getLocations(double latitude, double longitude, double radius);
    public Collection<Location> getLocations(boolean includeHidden);
    public Collection<Location> getLocations(double latitude, double longitude, double radius, boolean includeHidden);
    public Location addLocation(String name, double latitude, double longitude, double elevation);
    public void delete(Location location);
}

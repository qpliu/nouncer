package com.yrek.nouncer.dummy;

import java.util.ArrayList;
import java.util.Collection;

import com.yrek.nouncer.data.Location;
import com.yrek.nouncer.store.LocationStore;

class DummyLocationStore implements LocationStore {
    @Override
    public Collection<Location> getLocations(double latitude, double longitude, double radius) {
        ArrayList<Location> locations = new ArrayList<Location>();
        locations.add(UCSC_LOOP_1);
        locations.add(UCSC_LOOP_2);
        locations.add(UCSC_LOOP_3);
        locations.add(BONNY_DOON_1);
        locations.add(BONNY_DOON_2);
        locations.add(BONNY_DOON_3);
        locations.add(BONNY_DOON_4);
        locations.add(BONNY_DOON_5);
        locations.add(BONNY_DOON_6);
        locations.add(FELTON_EMPIRE_1);
        locations.add(FELTON_EMPIRE_2);
        locations.add(JAMISON_CREEK_1);
        locations.add(JAMISON_CREEK_2);
        locations.add(ALBA_1);
        locations.add(ALBA_2);
        return locations;
    }

    static final DummyLocation UCSC_LOOP_1 = new DummyLocation("UCSC Loop Start/End", 36.982162, -122.051004, 0.0);
    static final DummyLocation UCSC_LOOP_2 = new DummyLocation("UCSC Loop Mid-Climb", 36.991019, -122.054622, 0.0);
    static final DummyLocation UCSC_LOOP_3 = new DummyLocation("UCSC Loop Top of Climb", 36.998778, -122.054929, 0.0);

    static final DummyLocation BONNY_DOON_1 = new DummyLocation("Bonny Doon and Highway 1", 37.001067, -122.180379, 0.0);
    static final DummyLocation BONNY_DOON_2 = new DummyLocation("Bonny Doon and Smith Grade", 37.036684, -122.151924, 0.0);
    static final DummyLocation BONNY_DOON_3 = new DummyLocation("Bonny Doon and Pine Flat", 37.042163, -122.150826, 0.0);
    static final DummyLocation BONNY_DOON_4 = new DummyLocation("Pine Flat and Ice Cream Grade", 37.062808, -122.147769, 0.0);
    static final DummyLocation BONNY_DOON_5 = new DummyLocation("Pine Flat and Bonny Doon", 37.064184, -122.145301, 0.0);
    static final DummyLocation BONNY_DOON_6 = new DummyLocation("Pine Flat and Empire Grade", 37.078366, -122.133481, 0.0);

    static final DummyLocation FELTON_EMPIRE_1 = new DummyLocation("Felton-Empire and Highway 9", 37.053088, -122.073297, 0.0);
    static final DummyLocation FELTON_EMPIRE_2 = new DummyLocation("Felton-Empire and Empire Grade", 37.057959, -122.123125, 0.0);

    static final DummyLocation JAMISON_CREEK_1 = new DummyLocation("Jamison Creek and Highway 236", 37.146201, -122.157896, 0.0);
    static final DummyLocation JAMISON_CREEK_2 = new DummyLocation("Jamison Creek and Empire Grade", 37.141938, -122.187120, 0.0);

    static final DummyLocation ALBA_1 = new DummyLocation("Alba and Highway 9", 37.091851, -122.095929, 0.0);
    static final DummyLocation ALBA_2 = new DummyLocation("Alba and Empire Grade", 37.104120, -122.140899, 0.0);
}

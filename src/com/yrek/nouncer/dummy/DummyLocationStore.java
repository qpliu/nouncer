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
        locations.add(DOWNTOWN_1);
        locations.add(DOWNTOWN_2);
        return locations;
    }

    static final DummyLocation UCSC_LOOP_1 = new DummyLocation("UCSC Loop Start/End", 36.982162, -122.051004, 119.59938);
    static final DummyLocation UCSC_LOOP_2 = new DummyLocation("UCSC Loop Mid-Climb", 36.991019, -122.054622, 181.817093);
    static final DummyLocation UCSC_LOOP_3 = new DummyLocation("UCSC Loop Top of Climb", 36.998778, -122.054929, 228.564636);

    static final DummyLocation BONNY_DOON_1 = new DummyLocation("Bonny Doon and Highway 1", 37.001067, -122.180379, 16.867546);
    static final DummyLocation BONNY_DOON_2 = new DummyLocation("Bonny Doon and Smith Grade", 37.036684, -122.151924, 369.194885);
    static final DummyLocation BONNY_DOON_3 = new DummyLocation("Bonny Doon and Pine Flat", 37.042163, -122.150826, 384.891663);
    static final DummyLocation BONNY_DOON_4 = new DummyLocation("Pine Flat and Ice Cream Grade", 37.062808, -122.147769, 518.930603);
    static final DummyLocation BONNY_DOON_5 = new DummyLocation("Pine Flat and Bonny Doon", 37.064184, -122.145301, 541.006714);
    static final DummyLocation BONNY_DOON_6 = new DummyLocation("Pine Flat and Empire Grade", 37.078366, -122.133481, 662.273438);

    static final DummyLocation FELTON_EMPIRE_1 = new DummyLocation("Felton-Empire and Highway 9", 37.053088, -122.073297, 87.963921);
    static final DummyLocation FELTON_EMPIRE_2 = new DummyLocation("Felton-Empire and Empire Grade", 37.057959, -122.123125, 553.639099);

    static final DummyLocation JAMISON_CREEK_1 = new DummyLocation("Jamison Creek and Highway 236", 37.146201, -122.157896, 239.443588);
    static final DummyLocation JAMISON_CREEK_2 = new DummyLocation("Jamison Creek and Empire Grade", 37.141938, -122.187120, 698.49176);

    static final DummyLocation ALBA_1 = new DummyLocation("Alba and Highway 9", 37.091851, -122.095929, 112.316864);
    static final DummyLocation ALBA_2 = new DummyLocation("Alba and Empire Grade", 37.104120, -122.140899, 737.313721);

    static final DummyLocation DOWNTOWN_1 = new DummyLocation("Downtown 1", 36.973311, -122.025220, 3.220682);
    static final DummyLocation DOWNTOWN_2 = new DummyLocation("Downtown 2", 36.973328, -122.027082, 4.666704);

    static final DummyLocation WESTSIDE_1 = new DummyLocation("Mission and Walnut", 36.972379, -122.035402, 23.326372);
    static final DummyLocation WESTSIDE_2 = new DummyLocation("Mission and Laurel", 36.969572, -122.037328, 23.101624);
    static final DummyLocation WESTSIDE_3 = new DummyLocation("Mission and Bay", 36.966750, -122.040329, 23.253344);
    static final DummyLocation WESTSIDE_4 = new DummyLocation("Bay and King", 36.968205, -122.043027, 26.581438);
    static final DummyLocation WESTSIDE_5 = new DummyLocation("Bay and Nobel", 36.974002, -122.050264, 73.71833);
    static final DummyLocation WESTSIDE_6 = new DummyLocation("Bay and Meder", 36.975326, -122.052847, 84.718033);
    static final DummyLocation WESTSIDE_7 = new DummyLocation("Bay and High", 36.977147, -122.053680, 91.497444);
}

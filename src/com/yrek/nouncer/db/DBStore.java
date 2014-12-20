package com.yrek.nouncer.db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.yrek.nouncer.data.Location;
import com.yrek.nouncer.data.Route;
import com.yrek.nouncer.data.TrackPoint;
import com.yrek.nouncer.store.LocationStore;
import com.yrek.nouncer.store.RouteStore;
import com.yrek.nouncer.store.Store;
import com.yrek.nouncer.store.TrackStore;

public class DBStore implements Store {
    private static final int SCHEMA_VERSION = 1;

    private final SQLiteDatabase db;

    public DBStore(Context context) {
        this.db = new SQLiteOpenHelper(context, getClass().getName(), null, SCHEMA_VERSION) {
            @Override public void onCreate(SQLiteDatabase db) {
                db.execSQL("CREATE TABLE location (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, latitude REAL NOT NULL, longitude REAL NOT NULL, elevation REAL, hidden INTEGER NOT NULL DEFAULT 0)");
                db.execSQL("CREATE TABLE route (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, hidden INTEGER NOT NULL DEFAULT 0)");
                db.execSQL("CREATE TABLE route_point (route_id INTEGER REFERENCES route (id), location_id INTEGER REFERENCES location (id), index INTEGER, entry_announcement TEXT, exit_announcement TEXT), PRIMARY KEY (route_id, location_id, index)");
                db.execSQL("CREATE TABLE track (id INTEGER PRIMARY KEY AUTOINCREMENT, location_id INTEGER REFERENCES location (id), entry_time INTEGER NOT NULL, exit_time INTEGER)");
                insertInitialData(db);
            }
            @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            }
        }.getWritableDatabase();
    }

    private final LocationStore locationStore = new LocationStore() {
        @Override
        public Collection<Location> getLocations(double latitude, double longitude, double radius) {
            return new ArrayList<Location>();
        }
    };

    @Override
    public LocationStore getLocationStore() {
        return locationStore;
    }

    private final RouteStore routeStore = new RouteStore() {
        @Override
        public Collection<Route> getRoutes(Location location) {
            return new ArrayList<Route>();
        }

        @Override
        public Collection<Route> getRoutesStartingAt(Location location) {
            return new ArrayList<Route>();
        }
    };

    @Override
    public RouteStore getRouteStore() {
        return routeStore;
    }

    private final TrackStore trackStore = new TrackStore() {
        @Override
        public List<TrackPoint> getTrackPoints(long minTimestamp, long maxTimestamp) {
            return new ArrayList<TrackPoint>();
        }

        @Override
        public boolean addEntry(Location location, long timestamp) {
            return false;
        }

        @Override
        public boolean addExit(Location location, long timestamp) {
            return false;
        }
    };

    @Override
    public TrackStore getTrackStore() {
        return trackStore;
    }

    private static long insertLocation(SQLiteDatabase db, String name, double latitude, double longitude, double elevation) {
        return 0;
    }

    private static long insertRoute(SQLiteDatabase db, String name) {
        return 0;
    }

    private static long insertRoutePoint(SQLiteDatabase db, long routeId, long locationId, int index, String entryAnnouncement, String exitAnnouncement) {
        return 0;
    }

    private static void insertInitialData(SQLiteDatabase db) {
        long UCSC_LOOP_1 = insertLocation(db, "UCSC Loop Start/End", 36.982162, -122.051004, 119.59938);
        long UCSC_LOOP_2 = insertLocation(db, "UCSC Loop Mid-Climb", 36.991019, -122.054622, 181.817093);
        long UCSC_LOOP_3 = insertLocation(db, "UCSC Loop Top of Climb", 36.998778, -122.054929, 228.564636);

        long BONNY_DOON_1 = insertLocation(db, "Bonny Doon and Highway 1", 37.001067, -122.180379, 16.867546);
        long BONNY_DOON_2 = insertLocation(db, "Bonny Doon and Smith Grade", 37.036684, -122.151924, 369.194885);
        long BONNY_DOON_3 = insertLocation(db, "Bonny Doon and Pine Flat", 37.042163, -122.150826, 384.891663);
        long BONNY_DOON_4 = insertLocation(db, "Pine Flat and Ice Cream Grade", 37.062808, -122.147769, 518.930603);
        long BONNY_DOON_5 = insertLocation(db, "Pine Flat and Bonny Doon", 37.064184, -122.145301, 541.006714);
        long BONNY_DOON_6 = insertLocation(db, "Pine Flat and Empire Grade", 37.078366, -122.133481, 662.273438);

        long FELTON_EMPIRE_1 = insertLocation(db, "Felton-Empire and Highway 9", 37.053088, -122.073297, 87.963921);
        long FELTON_EMPIRE_2 = insertLocation(db, "Felton-Empire and Empire Grade", 37.057959, -122.123125, 553.639099);

        long JAMISON_CREEK_1 = insertLocation(db, "Jamison Creek and Highway 236", 37.146201, -122.157896, 239.443588);
        long JAMISON_CREEK_2 = insertLocation(db, "Jamison Creek and Empire Grade", 37.141938, -122.187120, 698.49176);

        long ALBA_1 = insertLocation(db, "Alba and Highway 9", 37.091851, -122.095929, 112.316864);
        long ALBA_2 = insertLocation(db, "Alba and Empire Grade", 37.104120, -122.140899, 737.313721);

        long DOWNTOWN_1 = insertLocation(db, "Soquel Parking Garage", 36.973311, -122.025220, 3.220682);
        long DOWNTOWN_2 = insertLocation(db, "Walnut and Cedar", 36.973328, -122.027082, 4.666704);
        long DOWNTOWN_3 = insertLocation(db, "Front and Cooper", 36.975199, -122.025509, 3.986676);
        long DOWNTOWN_4 = insertLocation(db, "Locust and Cedar", 36.975073, -122.027770, 5.656929);

        long WESTSIDE_1 = insertLocation(db, "Mission and Walnut", 36.972379, -122.035402, 23.326372);
        long WESTSIDE_2 = insertLocation(db, "Mission and Laurel", 36.969572, -122.037328, 23.101624);
        long WESTSIDE_3 = insertLocation(db, "Mission and Bay", 36.966750, -122.040329, 23.253344);
        long WESTSIDE_4 = insertLocation(db, "Bay and King", 36.968205, -122.043027, 26.581438);
        long WESTSIDE_5 = insertLocation(db, "Bay and Nobel", 36.974002, -122.050264, 73.71833);
        long WESTSIDE_6 = insertLocation(db, "Bay and Meder", 36.975326, -122.052847, 84.718033);
        long WESTSIDE_7 = insertLocation(db, "Bay and High", 36.977147, -122.053680, 91.497444);
        long WESTSIDE_8 = insertLocation(db, "High and Moore", 36.977876, -122.047838, 85.971718);
        long WESTSIDE_9 = insertLocation(db, "High and Laurent", 36.977703, -122.042535, 77.768517);
        long WESTSIDE_10 = insertLocation(db, "High and Storey", 36.977511, -122.035402, 45.688763);

        String ANNOUNCE_START = "%1$tl:%1$tM";
        String ANNOUNCE_ARRIVE = "%2$d minutes %3$02d seconds %2$d minutes %3$02d seconds %2$d minutes %3$02d seconds";

        long UCSC_LOOP = insertRoute(db, "UCSC Loop");
        insertRoutePoint(db, UCSC_LOOP, UCSC_LOOP_1, 0, null, ANNOUNCE_START);
        insertRoutePoint(db, UCSC_LOOP, UCSC_LOOP_2, 1, ANNOUNCE_ARRIVE, null);
        insertRoutePoint(db, UCSC_LOOP, UCSC_LOOP_3, 2, ANNOUNCE_ARRIVE, null);
        insertRoutePoint(db, UCSC_LOOP, UCSC_LOOP_1, 3, ANNOUNCE_ARRIVE, null);

        long BONNY_DOON = insertRoute(db, "Bonny Doon/Pine Flat");
        insertRoutePoint(db, BONNY_DOON, BONNY_DOON_1, 0, null, ANNOUNCE_START);
        insertRoutePoint(db, BONNY_DOON, BONNY_DOON_2, 1, ANNOUNCE_ARRIVE, null);
        insertRoutePoint(db, BONNY_DOON, BONNY_DOON_3, 2, ANNOUNCE_ARRIVE, null);
        insertRoutePoint(db, BONNY_DOON, BONNY_DOON_4, 3, ANNOUNCE_ARRIVE, null);
        insertRoutePoint(db, BONNY_DOON, BONNY_DOON_5, 4, ANNOUNCE_ARRIVE, null);
        insertRoutePoint(db, BONNY_DOON, BONNY_DOON_6, 5, ANNOUNCE_ARRIVE, null);

        long FELTON_EMPIRE = insertRoute(db, "Felton-Empire");
        insertRoutePoint(db, FELTON_EMPIRE, FELTON_EMPIRE_1, 0, null, ANNOUNCE_START);
        insertRoutePoint(db, FELTON_EMPIRE, FELTON_EMPIRE_2, 1, ANNOUNCE_ARRIVE, null);

        long JAMISON_CREEK = insertRoute(db, "Jamison Creek");
        insertRoutePoint(db, JAMISON_CREEK, JAMISON_CREEK_1, 0, null, ANNOUNCE_START);
        insertRoutePoint(db, JAMISON_CREEK, JAMISON_CREEK_2, 1, ANNOUNCE_ARRIVE, null);

        long ALBA = insertRoute(db, "Alba");
        insertRoutePoint(db, ALBA, ALBA_1, 0, null, ANNOUNCE_START);
        insertRoutePoint(db, ALBA, ALBA_2, 1, ANNOUNCE_ARRIVE, null);

        String ANNOUNCE_ENTER = "Enter %1$tl:%1$tM";
        String ANNOUNCE_EXIT = "Exit %1$tl:%1$tM";

        insertRoutePoint(db, insertRoute(db, "Downtown 1"), DOWNTOWN_1, 0, ANNOUNCE_ENTER, ANNOUNCE_EXIT);
        insertRoutePoint(db, insertRoute(db, "Downtown 2"), DOWNTOWN_2, 0, ANNOUNCE_ENTER, ANNOUNCE_EXIT);
        insertRoutePoint(db, insertRoute(db, "Downtown 3"), DOWNTOWN_3, 0, ANNOUNCE_ENTER, ANNOUNCE_EXIT);
        insertRoutePoint(db, insertRoute(db, "Downtown 4"), DOWNTOWN_4, 0, ANNOUNCE_ENTER, ANNOUNCE_EXIT);

        insertRoutePoint(db, insertRoute(db, "Westside 1"), WESTSIDE_1, 0, ANNOUNCE_ENTER, ANNOUNCE_EXIT);
        insertRoutePoint(db, insertRoute(db, "Westside 2"), WESTSIDE_2, 0, ANNOUNCE_ENTER, ANNOUNCE_EXIT);
        insertRoutePoint(db, insertRoute(db, "Westside 3"), WESTSIDE_3, 0, ANNOUNCE_ENTER, ANNOUNCE_EXIT);
        insertRoutePoint(db, insertRoute(db, "Westside 4"), WESTSIDE_4, 0, ANNOUNCE_ENTER, ANNOUNCE_EXIT);
        insertRoutePoint(db, insertRoute(db, "Westside 5"), WESTSIDE_5, 0, ANNOUNCE_ENTER, ANNOUNCE_EXIT);
        insertRoutePoint(db, insertRoute(db, "Westside 6"), WESTSIDE_6, 0, ANNOUNCE_ENTER, ANNOUNCE_EXIT);
        insertRoutePoint(db, insertRoute(db, "Westside 7"), WESTSIDE_7, 0, ANNOUNCE_ENTER, ANNOUNCE_EXIT);
        insertRoutePoint(db, insertRoute(db, "Westside 8"), WESTSIDE_8, 0, ANNOUNCE_ENTER, ANNOUNCE_EXIT);
        insertRoutePoint(db, insertRoute(db, "Westside 9"), WESTSIDE_9, 0, ANNOUNCE_ENTER, ANNOUNCE_EXIT);
        insertRoutePoint(db, insertRoute(db, "Westside 10"), WESTSIDE_10, 0, ANNOUNCE_ENTER, ANNOUNCE_EXIT);
    }
}

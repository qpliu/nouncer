package com.yrek.nouncer.db;

import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.JsonReader;

import com.yrek.nouncer.R;
import com.yrek.nouncer.data.Location;
import com.yrek.nouncer.data.Point;
import com.yrek.nouncer.data.Route;
import com.yrek.nouncer.data.RoutePoint;
import com.yrek.nouncer.data.TrackPoint;
import com.yrek.nouncer.processor.PointProcessor;
import com.yrek.nouncer.store.AvailabilityStore;
import com.yrek.nouncer.store.LocationStore;
import com.yrek.nouncer.store.PointStore;
import com.yrek.nouncer.store.RouteStore;
import com.yrek.nouncer.store.Store;
import com.yrek.nouncer.store.TrackStore;

public class DBStore implements Store {
    private static final int SCHEMA_VERSION = 1;
    private static final boolean STORE_POINTS = true;

    private final SQLiteDatabase db;

    public DBStore(final Context context) {
        this.db = new SQLiteOpenHelper(context, getClass().getName(), null, SCHEMA_VERSION) {
            @Override public void onCreate(SQLiteDatabase db) {
                db.execSQL("CREATE TABLE location (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, latitude REAL NOT NULL, longitude REAL NOT NULL, elevation REAL, hidden INTEGER NOT NULL DEFAULT 0)");
                db.execSQL("CREATE INDEX location_latitude ON location (latitude)");
                db.execSQL("CREATE INDEX location_longitude ON location (longitude)");

                db.execSQL("CREATE TABLE route (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, hidden INTEGER NOT NULL DEFAULT 0, starred INTEGER NOT NULL DEFAULT 0)");
                db.execSQL("CREATE TABLE route_point (route_id INTEGER REFERENCES route (id), location_id INTEGER REFERENCES location (id), route_index INTEGER, entry_announcement TEXT, exit_announcement TEXT, PRIMARY KEY (route_id, location_id, route_index))");

                db.execSQL("CREATE TABLE track (id INTEGER PRIMARY KEY AUTOINCREMENT, location_id INTEGER REFERENCES location (id), entry_time INTEGER NOT NULL, exit_time INTEGER, entry_heading FLOAT NOT NULL, exit_heading FLOAT, entry_speed FLOAT NOT NULL, exit_speed FLOAT, entry_timestamp INTEGER NOT NULL, exit_timestamp INTEGER)");
                db.execSQL("CREATE INDEX track_entry_time ON track (entry_time)");
                db.execSQL("CREATE INDEX track_exit_time ON track (exit_time)");

                db.execSQL("CREATE TABLE point (latitude REAL NOT NULL, longitude REAL NOT NULL, elevation REAL, time INTEGER NOT NULL, tag TEXT)");
                db.execSQL("CREATE INDEX point_time ON point (time)");

                db.execSQL("CREATE TABLE availability (unavailable_start_time INTEGER NOT NULL, unavailable_end_time INTEGER NOT NULL)");
                db.execSQL("CREATE INDEX availability_start ON availability (unavailable_start_time)");
                db.execSQL("CREATE INDEX availability_end ON availability (unavailable_end_time)");

                try {
                    insertInitialData(db, context.getResources());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            }
        }.getWritableDatabase();
    }

    private final LocationStore locationStore = new LocationStore() {
        @Override
        public Collection<Location> getLocations(double latitude, double longitude, double radius) {
            ArrayList<Location> list = new ArrayList<Location>();
            double dlat = PointProcessor.dlat(radius);
            double dlon = PointProcessor.dlon(latitude, radius);
            Cursor cursor = db.rawQuery("SELECT id, name, latitude, longitude, elevation FROM location WHERE hidden = 0 AND latitude >= ? AND latitude <= ? AND ((longitude >= ? AND longitude <= ?) OR (longitude >= ? AND longitude <= ?) OR (longitude >= ? AND longitude <= ?))", new String[] { String.valueOf(latitude - dlat),  String.valueOf(latitude + dlat), String.valueOf(longitude - dlon), String.valueOf(longitude + dlon), String.valueOf(longitude - dlon + 180.0), String.valueOf(longitude + dlon + 180.0), String.valueOf(longitude - dlon - 180.0), String.valueOf(longitude + dlon - 180.0) });
            try {
                while (cursor.moveToNext()) {
                    list.add(new DBLocation(cursor, 0));
                }
            } finally {
                cursor.close();
            }
            return list;
        }
    };

    private class DBLocation implements Location {
        private final long id;
        private final String name;
        private final double latitude;
        private final double longitude;
        private final double elevation;

        DBLocation(Cursor cursor, int column) {
            this.id = cursor.getLong(column + 0);
            this.name = cursor.getString(column + 1);
            this.latitude = cursor.getDouble(column + 2);
            this.longitude = cursor.getDouble(column + 3);
            this.elevation = cursor.isNull(column + 4) ? 0.0 : cursor.getDouble(column + 4);
        }

        @Override 
        public String getName() {
            return name;
        }

        @Override 
        public double getLatitude() {
            return latitude;
        }

        @Override 
        public double getLongitude() {
            return longitude;
        }

        @Override 
        public double getElevation() {
            return elevation;
        }

        @Override 
        public boolean equals(Object o) {
            return o instanceof DBLocation && ((DBLocation) o).id == id;
        }
    }

    @Override
    public LocationStore getLocationStore() {
        return locationStore;
    }

    private final RouteStore routeStore = new RouteStore() {
        @Override
        public Collection<Route> getRoutes() {
            ArrayList<Route> list = new ArrayList<Route>();
            Cursor cursor = db.rawQuery("SELECT id, name FROM route WHERE hidden = 0", null);
            try {
                while (cursor.moveToNext()) {
                    list.add(new DBRoute(cursor));
                }
            } finally {
                cursor.close();
            }
            return list;
        }

        @Override
        public Collection<Route> getRoutes(Location location) {
            long locationId = ((DBLocation) location).id;
            ArrayList<Route> list = new ArrayList<Route>();
            Cursor cursor = db.rawQuery("SELECT id, name FROM route WHERE hidden = 0 AND ? IN (SELECT location_id FROM route_point WHERE route_id = id)", new String[] { String.valueOf(locationId) });
            try {
                while (cursor.moveToNext()) {
                    list.add(new DBRoute(cursor));
                }
            } finally {
                cursor.close();
            }
            return list;
        }

        @Override
        public Collection<Route> getRoutesStartingAt(Location location) {
            long locationId = ((DBLocation) location).id;
            ArrayList<Route> list = new ArrayList<Route>();
            Cursor cursor = db.rawQuery("SELECT id, name FROM route, route_point WHERE hidden = 0 AND location_id = ? AND route_id = id AND route_index = 0", new String[] { String.valueOf(locationId) });
            try {
                while (cursor.moveToNext()) {
                    list.add(new DBRoute(cursor));
                }
            } finally {
                cursor.close();
            }
            return list;
        }
    };

    private HashMap<Long,Boolean> starredCache = new HashMap<Long,Boolean>();

    private class DBRoute implements Route {
        private final long id;
        private final String name;
        private final ArrayList<DBRoutePoint> routePoints;

        DBRoute(Cursor cursor) {
            this.id = cursor.getLong(0);
            this.name = cursor.getString(1);
            this.routePoints = new ArrayList<DBRoutePoint>();
            Cursor c = db.rawQuery("SELECT id, name, latitude, longitude, elevation, entry_announcement, exit_announcement FROM location, route_point WHERE route_id = ? AND id = location_id ORDER BY route_index ASC", new String[] { String.valueOf(id) });
            try {
                while (c.moveToNext()) {
                    routePoints.add(new DBRoutePoint(c));
                }
            } finally {
                c.close();
            }
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int getRoutePointCount() {
            return routePoints.size();
        }

        @Override
        public RoutePoint getRoutePoint(int index) {
            return routePoints.get(index);
        }

        @Override
        public List<RoutePoint> getRoutePoints() {
            return new ArrayList<RoutePoint>(routePoints);
        }

        @Override
        public boolean isStarred() {
            synchronized (starredCache) {
                if (starredCache.containsKey(id)) {
                    return starredCache.get(id);
                }
            }
            Cursor c = db.rawQuery("SELECT starred FROM route WHERE id = ?", new String[] { String.valueOf(id) });
            boolean starred = false;
            try {
                starred = c.moveToNext() && c.getInt(0) != 0;
            } finally {
                c.close();
            }
            synchronized (starredCache) {
                starredCache.put(id, starred);
            }
            return starred;
        }

        @Override
        public void setStarred(boolean starred) {
            ContentValues values = new ContentValues();
            values.put("starred", starred ? 1 : 0);
            db.update("route", values, "id = ?", new String[] { String.valueOf(id) });
            synchronized (starredCache) {
                starredCache.put(id, starred);
            }
        }
    }

    private class DBRoutePoint implements RoutePoint {
        private final Location location;
        private final String entryAnnouncement;
        private final String exitAnnouncement;

        DBRoutePoint(Cursor cursor) {
            this.location = new DBLocation(cursor, 0);
            this.entryAnnouncement = cursor.isNull(5) ? null : cursor.getString(5);
            this.exitAnnouncement = cursor.isNull(6) ? null : cursor.getString(6);
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

    @Override
    public PointStore getPointStore() {
        return pointStore;
    }

    private final PointStore pointStore = new PointStore() {
        @Override
        public List<Point> getPoints(long minTimestamp, long maxTimestamp, int maxPoints) {
            ArrayList<Point> list = new ArrayList<Point>();
            Cursor cursor = db.rawQuery("SELECT latitude, longitude, elevation, time FROM point WHERE time >= ? AND time <= ? ORDER BY time DESC", new String[] { String.valueOf(minTimestamp), String.valueOf(maxTimestamp) });
            try {
                while (cursor.moveToNext() && (maxPoints <= 0 || list.size() < maxPoints)) {
                    list.add(new DBPoint(cursor));
                }
            } finally {
                cursor.close();
            }
            return list;
        }

        @Override
        public boolean addPoint(Point point, String tag) {
            if (STORE_POINTS) {
                insertPoint(db, point.getLatitude(), point.getLongitude(), point.getElevation(), point.getTime(), tag);
            }
            return true;
        }
    };

    private class DBPoint implements Point {
        private final double latitude;
        private final double longitude;
        private final double elevation;
        private final long time;

        DBPoint(Cursor cursor) {
            this.latitude = cursor.getDouble(0);
            this.longitude = cursor.getDouble(1);
            this.elevation = cursor.getDouble(2);
            this.time = cursor.getLong(3);
        }

        @Override
        public double getLatitude() {
            return latitude;
        }

        @Override
        public double getLongitude() {
            return longitude;
        }

        @Override
        public double getElevation() {
            return elevation;
        }

        @Override
        public long getTime() {
            return time;
        }
    }

    @Override
    public RouteStore getRouteStore() {
        return routeStore;
    }

    private final TrackStore trackStore = new TrackStore() {
        @Override
        public List<TrackPoint> getTrackPoints(long minTimestamp, long maxTimestamp, int maxPoints) {
            ArrayList<TrackPoint> list = new ArrayList<TrackPoint>();
            Cursor cursor = db.rawQuery("SELECT location_id, name, latitude, longitude, elevation, entry_time, exit_time, entry_heading, exit_heading, entry_speed, exit_speed FROM location, track WHERE location.id = location_id AND entry_time >= ? AND entry_time <= ? ORDER BY entry_time DESC", new String[] { String.valueOf(minTimestamp), String.valueOf(maxTimestamp) });
            try {
                while (cursor.moveToNext() && (maxPoints <= 0 || list.size() < maxPoints)) {
                    list.add(new DBTrackPoint(cursor));
                }
            } finally {
                cursor.close();
            }
            return list;
        }

        @Override
        public boolean addEntry(Location location, long entryTime, double entryHeading, double entrySpeed, long timestamp) {
            insertTrackPoint(db, ((DBLocation) location).id, entryTime, entryHeading, entrySpeed, timestamp);
            return true;
        }

        @Override
        public boolean addExit(Location location, long exitTime, double exitHeading, double exitSpeed, long timestamp) {
            long id = 0L;
            Cursor cursor = db.rawQuery("SELECT id, location_id FROM track ORDER BY entry_time DESC", null);
            try {
                if (cursor.moveToNext() && ((DBLocation) location).id == cursor.getLong(1)) {
                    id = cursor.getLong(0);
                } else {
                    id = insertTrackPoint(db, ((DBLocation) location).id, exitTime, exitHeading, exitSpeed, timestamp);
                }
            } finally {
                cursor.close();
            }
            updateTrackPoint(db, id, exitTime, exitHeading, exitSpeed, timestamp);
            return true;
        }
    };

    private class DBTrackPoint implements TrackPoint {
        private final Location location;
        private final long entryTime;
        private final long exitTime;
        private final double entryHeading;
        private final double exitHeading;
        private final double entrySpeed;
        private final double exitSpeed;

        DBTrackPoint(Cursor cursor) {
            this.location = new DBLocation(cursor, 0);
            this.entryTime = cursor.getLong(5);
            this.exitTime = cursor.isNull(6) ? entryTime : cursor.getLong(6);
            this.entryHeading = cursor.getDouble(7);
            this.exitHeading = cursor.isNull(8) ? entryHeading : cursor.getDouble(8);
            this.entrySpeed = cursor.getDouble(9);
            this.exitSpeed = cursor.isNull(10) ? entryHeading : cursor.getDouble(10);
        }

        @Override
        public Location getLocation() {
            return location;
        }

        @Override
        public long getEntryTime() {
            return entryTime;
        }

        @Override
        public long getExitTime() {
            return exitTime;
        }

        @Override
        public double getEntryHeading() {
            return entryHeading;
        }

        @Override
        public double getExitHeading() {
            return exitHeading;
        }

        @Override
        public double getEntrySpeed() {
            return entrySpeed;
        }

        @Override
        public double getExitSpeed() {
            return exitSpeed;
        }
    }

    @Override
    public TrackStore getTrackStore() {
        return trackStore;
    }

    private final AvailabilityStore availabilityStore = new AvailabilityStore() {
        @Override
        public boolean wasUnavailable(long minTimestamp, long maxTimestamp) {
            Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM availability WHERE unavailable_start_time < ? AND unavailable_end_time > ?", new String[] { String.valueOf(maxTimestamp), String.valueOf(minTimestamp) });
            try {
                return cursor.moveToNext() && cursor.getInt(0) > 0;
            } finally {
                cursor.close();
            }
        }

        @Override
        public void addUnavailableTime(long startTime, long endTime) {
            ContentValues values = new ContentValues();
            values.put("unavailable_start_time", startTime);
            values.put("unavailable_end_time", endTime);
            db.insert("availability", null, values);
        }
    };

    @Override
    public AvailabilityStore getAvailabilityStore() {
        return availabilityStore;
    }

    private static long insertLocation(SQLiteDatabase db, String name, double latitude, double longitude, double elevation) {
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("latitude", latitude);
        values.put("longitude", longitude);
        values.put("elevation", elevation);
        return db.insert("location", null, values);
    }

    private static long insertRoute(SQLiteDatabase db, String name) {
        ContentValues values = new ContentValues();
        values.put("name", name);
        return db.insert("route", null, values);
    }

    private static long insertRoutePoint(SQLiteDatabase db, long routeId, long locationId, int index, String entryAnnouncement, String exitAnnouncement) {
        ContentValues values = new ContentValues();
        values.put("route_id", routeId);
        values.put("location_id", locationId);
        values.put("route_index", index);
        values.put("entry_announcement", entryAnnouncement);
        values.put("exit_announcement", exitAnnouncement);
        return db.insert("route_point", null, values);
    }

    private static long insertTrackPoint(SQLiteDatabase db, long locationId, long entryTime, double entryHeading, double entrySpeed, long timestamp) {
        ContentValues values = new ContentValues();
        values.put("location_id", locationId);
        values.put("entry_time", entryTime);
        values.put("entry_heading", entryHeading);
        values.put("entry_speed", entrySpeed);
        values.put("entry_timestamp", timestamp);
        return db.insert("track", null, values);
    }

    private static void updateTrackPoint(SQLiteDatabase db, long trackPointId, long exitTime, double exitHeading, double exitSpeed, long timestamp) {
        ContentValues values = new ContentValues();
        values.put("exit_time", exitTime);
        values.put("exit_heading", exitHeading);
        values.put("exit_speed", exitSpeed);
        values.put("exit_timestamp", timestamp);
        int count = db.update("track", values, "id = ?", new String[] { String.valueOf(trackPointId) });
    }

    private static long insertPoint(SQLiteDatabase db, double latitude, double longitude, double elevation, long time, String tag) {
        ContentValues values = new ContentValues();
        values.put("latitude", latitude);
        values.put("longitude", longitude);
        values.put("elevation", elevation);
        values.put("time", time);
        values.put("tag", tag);
        return db.insert("point", null, values);
    }

    private static void insertInitialData(SQLiteDatabase db, Resources resources) throws IOException {
        HashMap<String,Long> locations = new HashMap<String,Long>();
        JsonReader in = new JsonReader(new InputStreamReader(resources.openRawResource(R.raw.initial_locations), "UTF-8"));
        try {
            in.beginArray();
            while (in.hasNext()) {
                in.beginArray();
                String name = in.nextString();
                double lat = in.nextDouble();
                double lon = in.nextDouble();
                double elev = in.nextDouble();
                in.endArray();
                if (locations.containsKey(name)) {
                    throw new RuntimeException("duplicate location:"+name);
                }
                locations.put(name, insertLocation(db, name, lat, lon, elev));
            }
            in.endArray();
        } finally {
            in.close();
        }
        in = new JsonReader(new InputStreamReader(resources.openRawResource(R.raw.initial_routes), "UTF-8"));
        try {
            in.beginArray();
            while (in.hasNext()) {
                in.beginArray();
                String name = in.nextString();
                long routeId = insertRoute(db, name);
                String entryAnnouncement = in.nextString();
                String exitAnnouncement = in.nextString();
                in.beginArray();
                boolean hasNext = in.hasNext();
                for (int index = 0; hasNext; index++) {
                    String location = in.nextString();
                    hasNext = in.hasNext();
                    insertRoutePoint(db, routeId, locations.get(location), index, index > 0 || !hasNext ? entryAnnouncement : null, index == 0 ? exitAnnouncement : null);
                }
                in.endArray();
                in.endArray();
            }
            in.endArray();
        } finally {
            in.close();
        }
    }
}

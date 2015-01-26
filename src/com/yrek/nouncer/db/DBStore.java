package com.yrek.nouncer.db;

import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.yrek.nouncer.external.ExternalSource;
import com.yrek.nouncer.external.Link;
import com.yrek.nouncer.external.LinkStore;
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
    private final HashMap<Long,Boolean> locationHiddenCache = new HashMap<Long,Boolean>();
    private final HashMap<Long,Boolean> routeHiddenCache = new HashMap<Long,Boolean>();
    private final HashMap<Long,Boolean> routeStarredCache = new HashMap<Long,Boolean>();

    public DBStore(final Context context) {
        this.db = new SQLiteOpenHelper(context, getClass().getName(), null, SCHEMA_VERSION) {
            @Override public void onCreate(SQLiteDatabase db) {
                db.execSQL("CREATE TABLE location (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, latitude REAL NOT NULL, longitude REAL NOT NULL, elevation REAL, hidden INTEGER NOT NULL DEFAULT 0)");
                db.execSQL("CREATE INDEX location_latitude ON location (latitude)");
                db.execSQL("CREATE INDEX location_longitude ON location (longitude)");

                db.execSQL("CREATE TABLE route (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, hidden INTEGER NOT NULL DEFAULT 0, starred INTEGER NOT NULL DEFAULT 0)");
                db.execSQL("CREATE TABLE route_point (route_id INTEGER REFERENCES route (id), location_id INTEGER REFERENCES location (id), route_index INTEGER, entry_announcement TEXT, exit_announcement TEXT, PRIMARY KEY (route_id, route_index))");

                db.execSQL("CREATE TABLE track (id INTEGER PRIMARY KEY AUTOINCREMENT, location_id INTEGER REFERENCES location (id), entry_time INTEGER NOT NULL, exit_time INTEGER, entry_heading FLOAT NOT NULL, exit_heading FLOAT, entry_speed FLOAT NOT NULL, exit_speed FLOAT, entry_timestamp INTEGER NOT NULL, exit_timestamp INTEGER)");
                db.execSQL("CREATE INDEX track_entry_time ON track (entry_time)");
                db.execSQL("CREATE INDEX track_exit_time ON track (exit_time)");

                db.execSQL("CREATE TABLE point (latitude REAL NOT NULL, longitude REAL NOT NULL, elevation REAL, time INTEGER NOT NULL, tag TEXT)");
                db.execSQL("CREATE INDEX point_time ON point (time)");

                db.execSQL("CREATE TABLE availability (unavailable_start_time INTEGER NOT NULL, unavailable_end_time INTEGER NOT NULL)");
                db.execSQL("CREATE INDEX availability_start ON availability (unavailable_start_time)");
                db.execSQL("CREATE INDEX availability_end ON availability (unavailable_end_time)");

                db.execSQL("CREATE TABLE external_source (id INTEGER PRIMARY KEY AUTOINCREMENT, source_id TEXT NOT NULL)");
                db.execSQL("CREATE INDEX external_source_source_id ON external_source (source_id)");
                db.execSQL("CREATE TABLE external_source_attribute (external_source_id INTEGER REFERENCES external_source (id), key TEXT NOT NULL, value TEXT NOT NULL, PRIMARY KEY (external_source_id, key))");
                db.execSQL("CREATE INDEX external_source_attribute_external_source_id ON external_source_attribute (external_source_id)");

                db.execSQL("CREATE TABLE location_link (location_id INTEGER REFERENCES location (id), external_source_id INTEGER REFERENCES external_source (id), external_id TEXT NOT NULL, PRIMARY KEY (location_id, external_source_id, external_id))");
                db.execSQL("CREATE INDEX location_link_external_source_id ON location_link (external_source_id)");
                db.execSQL("CREATE TABLE location_link_attribute (location_id INTEGER REFERENCES location (id), external_source_id INTEGER REFERENCES external_source (id), external_id TEXT NOT NULL, key TEXT NOT NULL, value TEXT NOT NULL, PRIMARY KEY (location_id, external_source_id, external_id, key))");
                db.execSQL("CREATE INDEX location_link_attribute_link ON location_link_attribute (location_id, external_source_id, external_id)");
                db.execSQL("CREATE INDEX location_link_attribute_location_id ON location_link_attribute (location_id)");
                db.execSQL("CREATE INDEX location_link_attribute_external_source_id ON location_link_attribute (external_source_id)");

                db.execSQL("CREATE TABLE route_link (route_id INTEGER REFERENCES route (id), external_source_id INTEGER REFERENCES external_source (id), external_id TEXT NOT NULL, PRIMARY KEY (route_id, external_source_id, external_id))");
                db.execSQL("CREATE INDEX route_link_external_source_id ON route_link (external_source_id)");
                db.execSQL("CREATE TABLE route_link_attribute (route_id INTEGER REFERENCES route (id), external_source_id INTEGER REFERENCES external_source (id), external_id TEXT NOT NULL, key TEXT NOT NULL, value TEXT NOT NULL, PRIMARY KEY (route_id, external_source_id, external_id, key))");
                db.execSQL("CREATE INDEX route_link_attribute_link ON route_link_attribute (route_id, external_source_id, external_id)");
                db.execSQL("CREATE INDEX route_link_attribute_route_id ON route_link_attribute (route_id)");
                db.execSQL("CREATE INDEX route_link_attribute_external_source_id ON route_link_attribute (external_source_id)");

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
            return getLocations(latitude, longitude, radius, false);
        }

        @Override
        public Collection<Location> getLocations(double latitude, double longitude, double radius, boolean includeHidden) {
            double dlat = PointProcessor.dlat(radius);
            double dlon = PointProcessor.dlon(latitude, radius);
            return getLocations(latitude-dlat, latitude+dlat, longitude-dlon, longitude+dlon, includeHidden);
        }

        @Override
        public Collection<Location> getLocations(double minLatitude, double maxLatitude, double minLongitude, double maxLongitude, boolean includeHidden) {
            ArrayList<Location> list = new ArrayList<Location>();
            Cursor cursor = db.rawQuery("SELECT id, name, latitude, longitude, elevation FROM location WHERE " + (includeHidden ? "" : "hidden = 0 AND ") + "latitude >= ? AND latitude <= ? AND ((longitude >= ? AND longitude <= ?) OR (longitude >= ? AND longitude <= ?) OR (longitude >= ? AND longitude <= ?))", new String[] { String.valueOf(minLatitude), String.valueOf(maxLatitude), String.valueOf(minLongitude), String.valueOf(maxLongitude), String.valueOf(minLongitude + 180.0), String.valueOf(maxLongitude + 180.0), String.valueOf(minLongitude - 180.0), String.valueOf(maxLongitude - 180.0) });
            try {
                while (cursor.moveToNext()) {
                    list.add(new DBLocation(cursor, 0));
                }
            } finally {
                cursor.close();
            }
            return list;
        }

        @Override
        public Collection<Location> getLocations(boolean includeHidden) {
            ArrayList<Location> list = new ArrayList<Location>();
            Cursor cursor = db.rawQuery("SELECT id, name, latitude, longitude, elevation FROM location" + (includeHidden ? "" : " WHERE hidden = 0"), null);
            try {
                while (cursor.moveToNext()) {
                    list.add(new DBLocation(cursor, 0));
                }
            } finally {
                cursor.close();
            }
            return list;
        }

        @Override
        public Location addLocation(String name, double latitude, double longitude, double elevation) {
            long id = insertLocation(db, name, latitude, longitude, elevation);
            Cursor cursor = db.rawQuery("SELECT id, name, latitude, longitude, elevation FROM location WHERE id = ?", new String[] { String.valueOf(id) });
            try {
                cursor.moveToNext();
                return new DBLocation(cursor, 0);
            } finally {
                cursor.close();
            }
        }

        @Override
        public void delete(Location location) {
            db.beginTransaction();
            try {
                db.delete("location", "id = ?", new String[] { String.valueOf(((DBLocation) location).id) });
                db.delete("location_link", "location_id = ?", new String[] { String.valueOf(((DBLocation) location).id) });
                db.delete("location_link_attribute", "location_id = ?", new String[] { String.valueOf(((DBLocation) location).id) });
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    };

    private class DBLocation implements Location {
        private final long id;
        private String name;
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
        public void setName(String name) {
            this.name = name;
            ContentValues values = new ContentValues();
            values.put("name", name);
            db.update("location", values, "id = ?", new String[] { String.valueOf(id) });
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
        public boolean isHidden() {
            synchronized (locationHiddenCache) {
                if (locationHiddenCache.containsKey(id)) {
                    return locationHiddenCache.get(id);
                }
            }
            Cursor c = db.rawQuery("SELECT hidden FROM location WHERE id = ?", new String[] { String.valueOf(id) });
            boolean hidden = false;
            try {
                hidden = c.moveToNext() && c.getInt(0) != 0;
            } finally {
                c.close();
            }
            synchronized (locationHiddenCache) {
                locationHiddenCache.put(id, hidden);
            }
            return hidden;
        }

        @Override 
        public boolean equals(Object o) {
            return o instanceof DBLocation && ((DBLocation) o).id == id;
        }

        @Override
        public int hashCode() {
            return (int) id;
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
            Cursor cursor = db.rawQuery("SELECT id, name FROM route", null);
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
            Cursor cursor = db.rawQuery("SELECT id, name FROM route WHERE ? IN (SELECT location_id FROM route_point WHERE route_id = id)", new String[] { String.valueOf(locationId) });
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
            Cursor cursor = db.rawQuery("SELECT id, name FROM route, route_point WHERE location_id = ? AND route_id = id AND route_index = 0", new String[] { String.valueOf(locationId) });
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
        public void hideNonstarred() {
            db.beginTransaction();
            try {
                db.execSQL("UPDATE route SET hidden = 0");
                db.execSQL("UPDATE location SET hidden = 0");
                db.execSQL("UPDATE route SET hidden = 1 WHERE starred = 0");
                db.execSQL("UPDATE location SET hidden = 1 WHERE id NOT IN (SELECT location_id FROM route, route_point WHERE route_id = route.id AND location.id = location_id AND hidden = 0)");
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            synchronized (locationHiddenCache) {
                locationHiddenCache.clear();
            }
            synchronized (routeHiddenCache) {
                routeHiddenCache.clear();
            }
        }

        @Override
        public void unhideAll() {
            db.beginTransaction();
            try {
                db.execSQL("UPDATE location SET hidden = 0");
                db.execSQL("UPDATE route SET hidden = 0");
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            synchronized (locationHiddenCache) {
                locationHiddenCache.clear();
            }
            synchronized (routeHiddenCache) {
                routeHiddenCache.clear();
            }
        }

        @Override
        public void delete(Route route) {
            db.beginTransaction();
            try {
                db.delete("route_point", "route_id = ?", new String[] { String.valueOf(((DBRoute) route).id) });
                db.delete("route", "id = ?", new String[] { String.valueOf(((DBRoute) route).id) });
                db.delete("route_link", "route_id = ?", new String[] { String.valueOf(((DBRoute) route).id) });
                db.delete("route_link_attribute", "route_id = ?", new String[] { String.valueOf(((DBRoute) route).id) });
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        @Override
        public Route addRoute(String name, List<Location> locations) {
            db.beginTransaction();
            try {
                long id = insertRoute(db, name);
                for (int i = 0; i < locations.size(); i++) {
                    insertRoutePoint(db, id, ((DBLocation) locations.get(i)).id, i, null, null);
                }
                db.setTransactionSuccessful();
                return new DBRoute(id, name, locations);
            } finally {
                db.endTransaction();
            }
        }
    };


    private class DBRoute implements Route {
        private final long id;
        private String name;
        private final ArrayList<DBRoutePoint> routePoints;

        DBRoute(Cursor cursor) {
            this.id = cursor.getLong(0);
            this.name = cursor.getString(1);
            this.routePoints = new ArrayList<DBRoutePoint>();
            Cursor c = db.rawQuery("SELECT id, name, latitude, longitude, elevation, route_index FROM location, route_point WHERE route_id = ? AND id = location_id ORDER BY route_index ASC", new String[] { String.valueOf(id) });
            try {
                while (c.moveToNext()) {
                    routePoints.add(new DBRoutePoint(id, c));
                }
            } finally {
                c.close();
            }
        }

        DBRoute(long id, String name, List<Location> locations) {
            this.id = id;
            this.name = name;
            this.routePoints = new ArrayList<DBRoutePoint>();
            for (int i = 0; i < locations.size(); i++) {
                routePoints.add(new DBRoutePoint(id, i, locations.get(i)));
            }
        }

        @Override
        public String getName() {
            return name;
        }

        @Override 
        public void setName(String name) {
            this.name = name;
            ContentValues values = new ContentValues();
            values.put("name", name);
            db.update("route", values, "id = ?", new String[] { String.valueOf(id) });
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
            synchronized (routeStarredCache) {
                if (routeStarredCache.containsKey(id)) {
                    return routeStarredCache.get(id);
                }
            }
            Cursor c = db.rawQuery("SELECT starred FROM route WHERE id = ?", new String[] { String.valueOf(id) });
            boolean starred = false;
            try {
                starred = c.moveToNext() && c.getInt(0) != 0;
            } finally {
                c.close();
            }
            synchronized (routeStarredCache) {
                routeStarredCache.put(id, starred);
            }
            return starred;
        }

        @Override
        public void setStarred(boolean starred) {
            ContentValues values = new ContentValues();
            values.put("starred", starred ? 1 : 0);
            db.update("route", values, "id = ?", new String[] { String.valueOf(id) });
            synchronized (routeStarredCache) {
                routeStarredCache.put(id, starred);
            }
        }

        @Override
        public boolean isHidden() {
            synchronized (routeHiddenCache) {
                if (routeHiddenCache.containsKey(id)) {
                    return routeHiddenCache.get(id);
                }
            }
            Cursor c = db.rawQuery("SELECT hidden FROM route WHERE id = ?", new String[] { String.valueOf(id) });
            boolean hidden = false;
            try {
                hidden = c.moveToNext() && c.getInt(0) != 0;
            } finally {
                c.close();
            }
            synchronized (routeHiddenCache) {
                routeHiddenCache.put(id, hidden);
            }
            return hidden;
        }

        @Override 
        public boolean equals(Object o) {
            return o instanceof DBRoute && ((DBRoute) o).id == id;
        }

        @Override
        public int hashCode() {
            return (int) id;
        }
    }

    private class DBRoutePoint implements RoutePoint {
        private final long routeId;
        private final int routeIndex;
        private final Location location;

        DBRoutePoint(long routeId, Cursor cursor) {
            this.routeId = routeId;
            this.routeIndex = cursor.getInt(5);
            this.location = new DBLocation(cursor, 0);
        }

        DBRoutePoint(long routeId, int routeIndex, Location location) {
            this.routeId = routeId;
            this.routeIndex = routeIndex;
            this.location = location;
        }

        @Override
        public Location getLocation() {
            return location;
        }

        @Override
        public String getEntryAnnouncement() {
            Cursor c = db.rawQuery("SELECT entry_announcement FROM route_point WHERE route_id = ? AND route_index = ?", new String[] { String.valueOf(routeId), String.valueOf(routeIndex) });
            try {
                return c.moveToNext() && !c.isNull(0) ? c.getString(0) : null;
            } finally {
                c.close();
            }
        }

        @Override
        public String getExitAnnouncement() {
            Cursor c = db.rawQuery("SELECT exit_announcement FROM route_point WHERE route_id = ? AND route_index = ?", new String[] { String.valueOf(routeId), String.valueOf(routeIndex) });
            try {
                return c.moveToNext() && !c.isNull(0) ? c.getString(0) : null;
            } finally {
                c.close();
            }
        }

        @Override
        public void setEntryAnnouncement(String entryAnnouncement) {
            ContentValues values = new ContentValues();
            values.put("entry_announcement", entryAnnouncement);
            db.update("route_point", values, "route_id = ? AND route_index = ?", new String[] { String.valueOf(routeId), String.valueOf(routeIndex) });
        }

        @Override
        public void setExitAnnouncement(String exitAnnouncement) {
            ContentValues values = new ContentValues();
            values.put("exit_announcement", exitAnnouncement);
            db.update("route_point", values, "route_id = ? AND route_index = ?", new String[] { String.valueOf(routeId), String.valueOf(routeIndex) });
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
            long id;
            Cursor cursor = db.rawQuery("SELECT id FROM track WHERE exit_time IS NULL AND location_id = ? AND entry_time > ? ORDER BY entry_time DESC", new String[] { String.valueOf(((DBLocation) location).id), String.valueOf(timestamp - 8*3600*1000) });
            try {
                if (cursor.moveToNext()) {
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

    private class DBExternalSource implements ExternalSource {
        private final long id;
        private final String sourceId;

        DBExternalSource(Cursor cursor, int column) {
            this.id = cursor.getLong(column + 0);
            this.sourceId = cursor.getString(column + 1);
        }

        DBExternalSource(long id, String sourceId) {
            this.id = id;
            this.sourceId = sourceId;
        }

        @Override
        public String getExternalSourceId() {
            return sourceId;
        }

        @Override
        public Map<String,String> getAttributes() {
            HashMap<String,String> attrs = new HashMap<String,String>();
            Cursor cursor = db.rawQuery("SELECT key, value FROM external_source_attribute WHERE external_source_id = ?", new String[] { String.valueOf(id) });
            while (cursor.moveToNext()) {
                attrs.put(cursor.getString(0), cursor.getString(1));
            }
            return attrs;
        }

        @Override
        public void setAttribute(String key, String value) {
            if (value == null) {
                db.delete("external_source", "external_source_id = ? AND key = ?", new String[] { String.valueOf(id), key });
            } else {
                ContentValues values = new ContentValues();
                values.put("value", value);
                int count = db.update("external_source_attribute", values, "external_source_id = ? AND key = ?", new String[] { String.valueOf(id), key });
                if (count == 0) {
                    values.put("external_source_id", id);
                    values.put("key", key);
                    db.insert("external_source_attribute", null, values);
                }
            }
        }
    }

    private class DBLink<T> implements Link<T> {
        private final String tName;
        private final DBExternalSource externalSource;
        private final String externalId;
        private final long itemId;
        private final T item;

        DBLink(DBLinkStore<T> linkStore, DBExternalSource externalSource, Cursor cursor) {
            this.tName = linkStore.tName;
            this.externalSource = externalSource;
            this.externalId = cursor.getString(1);
            this.itemId = cursor.getLong(0);
            this.item = linkStore.item(itemId);
        }

        DBLink(DBLinkStore<T> linkStore, DBExternalSource externalSource, String externalId, long itemId, T item) {
            this.tName = linkStore.tName;
            this.externalSource = externalSource;
            this.externalId = externalId;
            this.itemId = itemId;
            this.item = item;
        }

        @Override
        public ExternalSource getExternalSource() {
            return externalSource;
        }

        @Override
        public String getExternalId() {
            return externalId;
        }

        @Override
        public Map<String,String> getAttributes() {
            HashMap<String,String> attrs = new HashMap<String,String>();
            Cursor cursor = db.rawQuery("SELECT key, value FROM "+tName+"_link_attribute WHERE "+tName+"_id = ? AND external_source_id = ? AND external_id = ?", new String[] { String.valueOf(itemId), String.valueOf(externalSource.id), externalId });
            while (cursor.moveToNext()) {
                attrs.put(cursor.getString(0), cursor.getString(1));
            }
            return attrs;
        }

        @Override
        public void setAttribute(String key, String value) {
            if (value == null) {
                db.delete(tName+"_link_attribute", tName+"_id = ? AND external_source_id = ? AND external_id = AND key = ?", new String[] { String.valueOf(itemId), String.valueOf(externalSource.id), externalId, key });
            } else {
                ContentValues values = new ContentValues();
                values.put("value", value);
                int count = db.update(tName+"_link_attribute", values, tName+"_id = ? AND external_source_id = ? AND external_id = ? AND key = ?", new String[] { String.valueOf(itemId), String.valueOf(externalSource.id), externalId, key });
                if (count == 0) {
                    values.put(tName+"_id", itemId);
                    values.put("external_source_id", externalSource.id);
                    values.put("external_id", externalId);
                    values.put("key", key);
                    db.insert(tName+"_link_attribute", null, values);
                }
            }
        }

        @Override
        public T getItem() {
            return item;
        }
    }

    private abstract class DBLinkStore<T> implements LinkStore<T> {
        private final String tName;

        DBLinkStore(String tName) {
            this.tName = tName;
        }

        protected abstract long itemId(T item);

        protected abstract T item(long itemId);

        @Override
        public Link<T> getLink(ExternalSource externalSource, String externalId) {
            Cursor cursor = db.rawQuery("SELECT "+tName+"_id, external_id FROM "+tName+"_link WHERE external_id = ? AND external_source_id = ?", new String[] { externalId, String.valueOf(((DBExternalSource) externalSource).id) });
            try {
                if (cursor.moveToNext()) {
                    return new DBLink<T>(this, (DBExternalSource) externalSource, cursor);
                }
                return null;
            } finally {
                cursor.close();
            }
        }

        @Override
        public Link<T> getLink(T item, ExternalSource externalSource) {
            Cursor cursor = db.rawQuery("SELECT "+tName+"_id, external_id FROM "+tName+"_link WHERE "+tName+"_id = ? AND external_source_id = ?", new String[] { String.valueOf(itemId(item)), String.valueOf(((DBExternalSource) externalSource).id) });
            try {
                if (cursor.moveToNext()) {
                    return new DBLink<T>(this, (DBExternalSource) externalSource, cursor);
                }
                return null;
            } finally {
                cursor.close();
            }
        }

        @Override
        public Collection<Link<T>> getLinks(T item) {
            ArrayList<Link<T>> list = new ArrayList<Link<T>>();
            Cursor cursor = db.rawQuery("SELECT "+tName+"_id, external_id, external_source_id, source_id FROM "+tName+"_link, external_source WHERE "+tName+"_id = ? AND external_source_id = external_source.id", new String[] { String.valueOf(itemId(item)) });
            try {
                while (cursor.moveToNext()) {
                    list.add(new DBLink<T>(this, new DBExternalSource(cursor, 2), cursor));
                }
                return list;
            } finally {
                cursor.close();
            }
        }

        @Override
        public void delete(Link<T> link) {
            DBLink<T> dbLink = (DBLink<T>) link;
            db.beginTransaction();
            try {
                db.delete(tName + "_link_attribute", tName + "_id = ? AND external_source_id = ? AND external_id = ?", new String[] { String.valueOf(dbLink.itemId), String.valueOf(dbLink.externalSource.id), dbLink.externalId });
                db.delete(tName + "_link", tName + "_id = ? AND external_source_id = ? AND external_id = ?", new String[] { String.valueOf(dbLink.itemId), String.valueOf(dbLink.externalSource.id), dbLink.externalId });
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        @Override
        public Link<T> addLink(T item, ExternalSource externalSource, String externalId) {
            ContentValues values = new ContentValues();
            values.put(tName+"_id", itemId(item));
            values.put("external_source_id", ((DBExternalSource) externalSource).id);
            values.put("external_id", externalId);
            db.insert(tName+"_link", null, values);
            return new DBLink<T>(this, (DBExternalSource) externalSource, externalId, itemId(item), item);
        }

        @Override
        public ExternalSource getExternalSource(String externalSourceId) {
            Cursor cursor = db.rawQuery("SELECT id, source_id FROM external_source WHERE source_id = ?", new String[] { externalSourceId });
            try {
                if (cursor.moveToNext()) {
                    return new DBExternalSource(cursor, 0);
                }
                return null;
            } finally {
                cursor.close();
            }
        }

        @Override
        public Collection<ExternalSource> getExternalSources() {
            ArrayList<ExternalSource> list = new ArrayList<ExternalSource>();
            Cursor cursor = db.rawQuery("SELECT id, source_id FROM external_source", null);
            try {
                while (cursor.moveToNext()) {
                    list.add(new DBExternalSource(cursor, 0));
                }
                return list;
            } finally {
                cursor.close();
            }
        }

        @Override
        public void delete(ExternalSource externalSource) {
            db.beginTransaction();
            try {
                db.delete("external_source", "id = ?", new String[] { String.valueOf(((DBExternalSource) externalSource).id) });
                db.delete("external_source_attribute", "external_source_id = ?", new String[] { String.valueOf(((DBExternalSource) externalSource).id) });
                db.delete("location_link", "external_source_id = ?", new String[] { String.valueOf(((DBExternalSource) externalSource).id) });
                db.delete("location_link_attribute", "external_source_id = ?", new String[] { String.valueOf(((DBExternalSource) externalSource).id) });
                db.delete("route_link", "external_source_id = ?", new String[] { String.valueOf(((DBExternalSource) externalSource).id) });
                db.delete("route_link_attribute", "external_source_id = ?", new String[] { String.valueOf(((DBExternalSource) externalSource).id) });
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        @Override
        public ExternalSource addExternalSource(String externalSourceId) {
            ContentValues values = new ContentValues();
            values.put("source_id", externalSourceId);
            return new DBExternalSource(db.insert("external_source", null, values), externalSourceId);
        }
    }

    private final DBLinkStore<Location> locationLinkStore = new DBLinkStore<Location>("location") {
        @Override
        protected long itemId(Location item) {
            return ((DBLocation) item).id;
        }

        @Override
        protected Location item(long itemId) {
            Cursor cursor = db.rawQuery("SELECT id, name, latitude, longitude, elevation FROM location WHERE id = ?", new String[] { String.valueOf(itemId) });
            try {
                if (cursor.moveToNext()) {
                    return new DBLocation(cursor, 0);
                }
                return null;
            } finally {
                cursor.close();
            }
        }
    };

    @Override
    public LinkStore<Location> getLocationLinkStore() {
        return locationLinkStore;
    }

    private final DBLinkStore<Route> routeLinkStore = new DBLinkStore<Route>("route") {
        @Override
        protected long itemId(Route item) {
            return ((DBRoute) item).id;
        }

        @Override
        protected Route item(long itemId) {
            Cursor cursor = db.rawQuery("SELECT id, name FROM route WHERE id = ?", new String[] { String.valueOf(itemId) });
            try {
                if (cursor.moveToNext()) {
                    return new DBRoute(cursor);
                }
                return null;
            } finally {
                cursor.close();
            }
        }
    };

    @Override
    public LinkStore<Route> getRouteLinkStore() {
        return routeLinkStore;
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

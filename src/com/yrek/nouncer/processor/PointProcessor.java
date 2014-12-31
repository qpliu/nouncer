package com.yrek.nouncer.processor;

import com.yrek.nouncer.data.Location;
import com.yrek.nouncer.data.Point;

import com.yrek.nouncer.store.LocationStore;

public class PointProcessor implements PointReceiver {
    private final LocationStore locationStore;
    private final Listener listener;
    private final double entryRadius;
    // exitRadius must be greater than entryRadius
    private final double exitRadius;

    // Does not handle locations nearer than exitRadius of each other.
    // Supporting closer locations would take something more complicated,
    // should the need arise.
    private Location proximateLocation = null;
    private boolean entered = false;
    private Point lastPoint = null;
    private double lastDistance = 0.0;

    public PointProcessor(LocationStore locationStore, Listener listener) {
        this.locationStore = locationStore;
        this.listener = listener;
        this.entryRadius = 30.0;
        this.exitRadius = 40.0;
    }

    @Override
    public void receivePoint(Point point) {
        if (lastPoint != null && point.getTime() - lastPoint.getTime() < 1000L) {
            return;
        }
        if (proximateLocation != null) {
            double d = distance(point, proximateLocation);
            if (d >= exitRadius) {
                if (lastDistance < exitRadius && entered) {
                    sendExit(proximateLocation, point, lastPoint);
                }
                proximateLocation = null;
                entered = false;
            } else {
                if (d < entryRadius && lastDistance >= entryRadius && !entered) {
                    entered = true;
                    sendEntry(proximateLocation, point, lastPoint);
                }
                lastPoint = point;
                lastDistance = d;
                return;
            }
        }
        lastDistance = exitRadius;
        for (Location l : locationStore.getLocations(point.getLatitude(), point.getLongitude(), exitRadius)) {
            double d = distance(point, l);
            if (d < lastDistance) {
                lastDistance = d;
                proximateLocation = l;
            }
        }
        if (lastDistance < entryRadius) {
            if (lastPoint == null && !entered) {
                entered = true;
                sendEntry(proximateLocation, point, null);
            } else {
                double d = distance(lastPoint, proximateLocation);
                if (d > lastDistance && !entered) {
                    entered = true;
                    sendEntry(proximateLocation, point, lastPoint);
                }
            }
        }
        lastPoint = point;
    }

    private void sendExit(Location proximateLocation, Point point, Point lastPoint) {
        listener.receiveExit(proximateLocation, extrapolateTime(lastPoint, point, proximateLocation), heading(proximateLocation, point), speed(lastPoint, point), point.getTime());
    }

    private void sendEntry(Location proximateLocation, Point point, Point lastPoint) {
        long time;
        double heading;
        double speed;
        if (lastPoint == null) {
            time = point.getTime();
            heading = heading(point, proximateLocation);
            speed = 0.0;
        } else {
            time = extrapolateTime(lastPoint, point, proximateLocation);
            if (time > point.getTime()) {
                heading = heading(point, proximateLocation);
            } else if (time < point.getTime()) {
                heading = heading(proximateLocation, point);
            } else {
                heading = heading(lastPoint, point);
            }
            speed = speed(lastPoint, point);
        }
        listener.receiveEntry(proximateLocation, time, heading, speed, point.getTime());
    }

    public interface Listener {
        public void receiveEntry(Location location, long entryTime, double entryHeading, double entrySpeed, long timestamp);
        public void receiveExit(Location location, long exitTime, double exitHeading, double exitSpeed, long timestamp);
    }

    // p2 must be closer to location than p1
    private static long extrapolateTime(Point p1, Point p2, Location location) {
        long dt = p2.getTime() > p1.getTime() ? 1000L : -1000L;
        double dlat = (p2.getLatitude() - p1.getLatitude()) / (double) (p2.getTime() - p1.getTime()) * ((double) dt);
        double dlon = (p2.getLongitude() - p1.getLongitude()) / (double) (p2.getTime() - p1.getTime()) * ((double) dt);
        long t = p1.getTime();
        // also interpolate in case location is between p1 and p2
        double lat = p1.getLatitude();
        double lon = p1.getLongitude();
        double dist = distance(p1, location);
        for (;;) {
            lat += dlat;
            lon += dlon;
            double d = distance(lat, lon, location.getLatitude(), location.getLongitude());
            if (d > dist) {
                return t;
            }
            dist = d;
            t += dt;
        }
    }

    public static final double R = 6371000;

    public static double distance(double lat1, double lon1, double lat2, double lon2) {
        double sdlat2 = Math.sin((lat1 - lat2)/2.0*Math.PI/180.0);
        double sdlon2 = Math.sin((lon1 - lon2)/2.0*Math.PI/180.0);
        double c1 = Math.cos(lat1*Math.PI/180.0);
        double c2 = Math.cos(lat2*Math.PI/180.0);
        return 2.0*R*Math.asin(Math.sqrt(sdlat2*sdlat2 + c1*c2*sdlon2*sdlon2));
    }

    public static double distance(Point p1, Point p2) {
        return distance(p1.getLatitude(), p1.getLongitude(), p2.getLatitude(), p2.getLongitude());
    }

    public static double distance(Point p, Location l) {
        return distance(p.getLatitude(), p.getLongitude(), l.getLatitude(), l.getLongitude());
    }

    public static double dlat(double dist) {
        return dist/R*180.0/Math.PI;
    }

    public static double dlon(double lat, double dist) {
        return dist/(R*Math.cos(lat*Math.PI/180.0))*180.0/Math.PI;
    }

    public static double heading(Point p1, Point p2) {
        return heading(p1.getLatitude(), p1.getLongitude(), p2.getLatitude(), p2.getLongitude());
    }

    public static double heading(Point p, Location l) {
        return heading(p.getLatitude(), p.getLongitude(), l.getLatitude(), l.getLongitude());
    }

    public static double heading(Location l, Point p) {
        return heading(l.getLatitude(), l.getLongitude(), p.getLatitude(), p.getLongitude());
    }

    public static double heading(double lat1, double lon1, double lat2, double lon2) {
        double dx = distance(lat1, lon1, lat1, lon2);
        double dy = distance(lat1, lon1, lat2, lon1);
        if (lon1 > lon2) {
            dx = -dx;
        }
        if (lat1 > lat2) {
            dy = -dy;
        }
        return 180.0/Math.PI*Math.atan2(dx, dy);
    }

    public static double speed(Point p1, Point p2) {
        if (p1.getTime() == p2.getTime()) {
            return 0.0;
        } else {
            return 1000.0*distance(p1, p2)/((double) (p2.getTime() - p1.getTime()));
        }
    }
}

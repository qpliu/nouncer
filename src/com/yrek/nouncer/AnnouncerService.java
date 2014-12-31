package com.yrek.nouncer;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.yrek.nouncer.data.Location;
import com.yrek.nouncer.data.Point;
import com.yrek.nouncer.data.Route;
import com.yrek.nouncer.db.DBStore;
import com.yrek.nouncer.processor.PointProcessor;
import com.yrek.nouncer.processor.PointReceiver;
import com.yrek.nouncer.processor.RouteProcessor;
import com.yrek.nouncer.store.Store;
import com.yrek.nouncer.store.RouteStore;
import com.yrek.nouncer.store.TrackStore;

public class AnnouncerService extends Service {
    private Announcer announcer = null;
    private LocationSource locationSource = null;
    private Store store;

    private PointReceiver pointListener = null;
    private PointProcessor.Listener locationListener = null;
    private RouteProcessor.Listener routeListener = null;

    public class LocalBinder extends Binder {
        AnnouncerService getService() {
            return AnnouncerService.this;
        }
    }

    private final LocalBinder binder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        store = new DBStore(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (locationSource != null) {
            locationSource.stop();
            locationSource = null;
        }
        if (announcer != null) {
            announcer.stop();
            announcer = null;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (announcer == null) {
            announcer = new Announcer(this);
            announcer.start();
        }
        if (locationSource == null) {
            final RouteProcessor routeProcessor = new RouteProcessor(store.getRouteStore(), store.getTrackStore(), new RouteProcessor.Listener() {
                @Override public void receiveEntry(Route route, long startTime, int routeIndex, long entryTime, double entryHeading, double entrySpeed) {
                    announcer.receiveEntry(route, startTime, routeIndex, entryTime, entryHeading, entrySpeed);
                    RouteProcessor.Listener listener = routeListener;
                    if (listener != null) {
                        listener.receiveEntry(route, startTime, routeIndex, entryTime, entryHeading, entrySpeed);
                    }
                }
                @Override public void receiveExit(Route route, long startTime, int routeIndex, long exitTime, double exitHeading, double exitSpeed) {
                    announcer.receiveExit(route, startTime, routeIndex, exitTime, exitHeading, exitSpeed);
                    RouteProcessor.Listener listener = routeListener;
                    if (listener != null) {
                        listener.receiveExit(route, startTime, routeIndex, exitTime, exitHeading, exitSpeed);
                    }
                }
            });

            final PointProcessor pointProcessor = new PointProcessor(store.getLocationStore(), new PointProcessor.Listener() {
                @Override public void receiveEntry(Location location, long entryTime, double entryHeading, double entrySpeed, long timestamp) {
                    store.getTrackStore().addEntry(location, entryTime, entryHeading, entrySpeed, timestamp);
                    routeProcessor.receiveEntry(location, entryTime, entryHeading, entrySpeed, timestamp);
                    PointProcessor.Listener listener = locationListener;
                    if (listener != null) {
                        listener.receiveEntry(location, entryTime, entryHeading, entrySpeed, timestamp);
                    }
                }
                @Override public void receiveExit(Location location, long exitTime, double exitHeading, double exitSpeed, long timestamp) {
                    store.getTrackStore().addExit(location, exitTime, exitHeading, exitSpeed, timestamp);
                    routeProcessor.receiveExit(location, exitTime, exitHeading, exitSpeed, timestamp);
                    PointProcessor.Listener listener = locationListener;
                    if (listener != null) {
                        listener.receiveExit(location, exitTime, exitHeading, exitSpeed, timestamp);
                    }
                }
            });

            locationSource = new LocationSource(this, store.getLocationStore(), store.getPointStore(), new PointReceiver() {
                @Override public void receivePoint(Point point) {
                    try {
                        pointProcessor.receivePoint(point);
                        PointReceiver listener = pointListener;
                        if (listener != null) {
                            listener.receivePoint(point);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            locationSource.start();
        }
        return Service.START_STICKY;
    }

    void setListeners(PointReceiver pointListener, PointProcessor.Listener locationListener, RouteProcessor.Listener routeListener) {
        this.pointListener = pointListener;
        this.locationListener = locationListener;
        this.routeListener = routeListener;
    }

    public boolean isStarted() {
        return locationSource != null;
    }

    public void stop() {
        if (locationSource != null) {
            locationSource.stop();
            locationSource = null;
        }
        if (announcer != null) {
            announcer.stop();
            announcer = null;
        }
        stopSelf();
    }

    public TrackStore getTrackStore() {
        return store.getTrackStore();
    }

    public RouteStore getRouteStore() {
        return store.getRouteStore();
    }
}

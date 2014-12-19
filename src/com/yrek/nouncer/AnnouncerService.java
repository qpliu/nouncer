package com.yrek.nouncer;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.yrek.nouncer.data.Location;
import com.yrek.nouncer.data.Point;
import com.yrek.nouncer.data.Route;
import com.yrek.nouncer.dummy.DummyStore;
import com.yrek.nouncer.processor.PointProcessor;
import com.yrek.nouncer.processor.PointReceiver;
import com.yrek.nouncer.processor.RouteProcessor;
import com.yrek.nouncer.store.Store;
import com.yrek.nouncer.store.TrackStore;

public class AnnouncerService extends Service {
    private Announcer announcer = null;
    private LocationSource locationSource = null;
    private Store store = new DummyStore();

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
            final RouteProcessor routeProcessor = new RouteProcessor(store.getRouteStore(), new RouteProcessor.Listener() {
                @Override public void receiveEntry(final Route route, final long startTime, final int routeIndex, final long timestamp) {
                    
                    announcer.receiveEntry(route, startTime, routeIndex, timestamp);
                    RouteProcessor.Listener listener = routeListener;
                    if (listener != null) {
                        listener.receiveEntry(route, startTime, routeIndex, timestamp);
                    }
                }
                @Override public void receiveExit(final Route route, final long startTime, final int routeIndex, final long timestamp) {
                    announcer.receiveExit(route, startTime, routeIndex, timestamp);
                    RouteProcessor.Listener listener = routeListener;
                    if (listener != null) {
                        listener.receiveExit(route, startTime, routeIndex, timestamp);
                    }
                }
            });

            final PointProcessor pointProcessor = new PointProcessor(store.getLocationStore(), new PointProcessor.Listener() {
                @Override public void receiveEntry(final Location location, final long timestamp) {
                    routeProcessor.receiveEntry(location, timestamp);
                    store.getTrackStore().addEntry(location, timestamp);
                    PointProcessor.Listener listener = locationListener;
                    if (listener != null) {
                        listener.receiveEntry(location, timestamp);
                    }
                }
                @Override public void receiveExit(final Location location, final long timestamp) {
                    routeProcessor.receiveExit(location, timestamp);
                    store.getTrackStore().addExit(location, timestamp);
                    PointProcessor.Listener listener = locationListener;
                    if (listener != null) {
                        listener.receiveExit(location, timestamp);
                    }
                }
            });

            locationSource = new LocationSource(this, store.getLocationStore(), new PointReceiver() {
                @Override public void receivePoint(final Point point) {
                    pointProcessor.receivePoint(point);
                    PointReceiver listener = pointListener;
                    if (listener != null) {
                        listener.receivePoint(point);
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

    boolean isStarted() {
        return locationSource != null;
    }

    void stop() {
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

    TrackStore getTrackStore() {
        return store.getTrackStore();
    }
}

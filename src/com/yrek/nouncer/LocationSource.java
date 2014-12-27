package com.yrek.nouncer;

import android.content.Context;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;

import com.yrek.nouncer.data.Location;
import com.yrek.nouncer.data.Point;
import com.yrek.nouncer.processor.PointProcessor;
import com.yrek.nouncer.processor.PointReceiver;
import com.yrek.nouncer.store.LocationStore;

class LocationSource {
    private final Context context;
    private final LocationStore locationStore;
    private final PointReceiver pointReceiver;
    private HandlerThread handlerThread = null;

    LocationSource(Context context, LocationStore locationStore, PointReceiver pointReceiver) {
        this.context = context;
        this.locationStore = locationStore;
        this.pointReceiver = pointReceiver;
    }

    public void start() {
        handlerThread = new HandlerThread("LocationSource");
        handlerThread.start();
        listener.start();
    }

    public void stop() {
        new Handler(handlerThread.getLooper()).post(new Runnable() {
            @Override public void run() {
                listener.stop();
                handlerThread.quit();
            }
        });
    }

    private final Listener listener = new Listener(0, null);

    private static class ListenerParameters {
        final long updateMinTime;
        final float updateMinDistance;
        final double addListenerDistance;
        final double shutdownDistance;

        ListenerParameters(long updateMinTime, float updateMinDistance, double addListenerDistance, double shutdownDistance) {
            this.updateMinTime = updateMinTime;
            this.updateMinDistance = updateMinDistance;
            this.addListenerDistance = addListenerDistance;
            this.shutdownDistance = shutdownDistance;
        }
    }

    private static final ListenerParameters[] LISTENER_PARAMETERS = new ListenerParameters[] {
        new ListenerParameters(60000L, 100.0f, 1000.0, 1000000.0),
        new ListenerParameters(30000L, 100.0f,  500.0,    1000.0),
        new ListenerParameters(10000L,  10.0f,   50.0,     500.0),
        new ListenerParameters( 2000L,   1.0f,    0.0,      50.0),
    };

    private class Listener implements LocationListener {
        private final int level;
        private final Listener parent;
        private Listener child = null;

        Listener(int level, Listener parent) {
            this.level = level;
            this.parent = parent;
        }

        public void start() {
            ((LocationManager) context.getSystemService(Context.LOCATION_SERVICE)).requestLocationUpdates(LocationManager.GPS_PROVIDER, LISTENER_PARAMETERS[level].updateMinTime, LISTENER_PARAMETERS[level].updateMinDistance, this, handlerThread.getLooper());
        }

        public void stop() {
            if (child != null) {
                child.stop();
            }
            assert child == null;
            if (parent != null) {
                assert parent.child == this;
                parent.child = null;
            }
            ((LocationManager) context.getSystemService(Context.LOCATION_SERVICE)).removeUpdates(this);
        }

        @Override
        public void onLocationChanged(final android.location.Location location) {
            if (child != null) {
                return;
            }
            final long time = System.currentTimeMillis();
            Point point = new Point() {
                @Override public double getLatitude() {
                    return location.getLatitude();
                }

                @Override public double getLongitude() {
                    return location.getLongitude();
                }

                @Override public double getElevation() {
                    return location.getAltitude();
                }

                @Override public long getTime() {
                    return time;
                }
            };
            pointReceiver.receivePoint(point);
            double dist = LISTENER_PARAMETERS[level].shutdownDistance;
            for (Location l : locationStore.getLocations(location.getLatitude(), location.getLongitude(), Math.min(dist, LISTENER_PARAMETERS[0].addListenerDistance))) {
                dist = Math.min(dist, PointProcessor.distance(point, l));
            }
            if (level > 0 && dist >= LISTENER_PARAMETERS[level].shutdownDistance) {
                stop();
            }
            Listener l = this;
            while (l.child == null && l.level < LISTENER_PARAMETERS.length && dist < LISTENER_PARAMETERS[l.level].addListenerDistance) {
                l.child = new Listener(l.level + 1, l);
                l.child.start();
                l = l.child;
            }
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    }
}

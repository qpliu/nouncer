package com.yrek.nouncer;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.TextView;

import com.yrek.nouncer.data.Location;
import com.yrek.nouncer.data.Point;
import com.yrek.nouncer.data.Route;
import com.yrek.nouncer.processor.PointProcessor;
import com.yrek.nouncer.processor.PointReceiver;
import com.yrek.nouncer.processor.RouteProcessor;
import com.yrek.nouncer.store.TrackStore;

public class Main extends Activity {
    private RouteProcessor.Listener routeListener;
    private PointProcessor.Listener locationListener;
    private PointReceiver pointListener;
    private ServiceConnection serviceConnection;
    private TrackStore trackStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        final TextView routeText = (TextView) findViewById(R.id.route_text);
        routeListener = new RouteProcessor.Listener() {
            @Override public void receiveEntry(final Route route, final long startTime, final int routeIndex, final long timestamp) {
                routeText.post(new Runnable() {
                    @Override public void run() {
                        routeText.setText(String.format("Entry: Route: %s Location %d: %s %tR %d:%02d", route.getName(), routeIndex, route.getRoutePoint(routeIndex).getLocation().getName(), timestamp, (timestamp - startTime) / 60000L, (timestamp - startTime) % 60000L / 1000L));
                    }
                });
            }
            @Override public void receiveExit(final Route route, final long startTime, final int routeIndex, final long timestamp) {
                routeText.post(new Runnable() {
                    @Override public void run() {
                        routeText.setText(String.format("Exit: Route: %s Location %d: %s %tR %d:%02d", route.getName(), routeIndex, route.getRoutePoint(routeIndex).getLocation().getName(), timestamp, (timestamp - startTime) / 60000L, (timestamp - startTime) % 60000L / 1000L));
                    }
                });
            }
        };

        final TextView locationText = (TextView) findViewById(R.id.location_text);
        locationListener = new PointProcessor.Listener() {
            @Override public void receiveEntry(final Location location, final long timestamp) {
                locationText.post(new Runnable() {
                    @Override public void run() {
                        locationText.setText(String.format("Entry: Location: %s %tR", location.getName(), timestamp));
                    }
                });
            }
            @Override public void receiveExit(final Location location, final long timestamp) {
                locationText.post(new Runnable() {
                    @Override public void run() {
                        locationText.setText(String.format("Exit: Location: %s %tR", location.getName(), timestamp));
                    }
                });
            }
        };

        final TextView pointText = (TextView) findViewById(R.id.point_text);
        pointListener = new PointReceiver() {
            @Override public void receivePoint(final Point point) {
                pointText.post(new Runnable() {
                    @Override public void run() {
                        pointText.setText(String.format("Point: %tT: %f,%f,%.1f", point.getTime(), point.getLatitude(), point.getLongitude(), point.getElevation()));
                    }
                });
            }
        };
        pointText.setText("Point: None");
    }

    @Override
    protected void onStart() {
        super.onStart();
        startService(new Intent(this, AnnouncerService.class));
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopService(new Intent(this, AnnouncerService.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
        serviceConnection = new ServiceConnection() {
            private AnnouncerService announcerService = null;
            @Override public void onServiceConnected(ComponentName name, IBinder service) {
                announcerService = ((AnnouncerService.LocalBinder) service).getService();
                announcerService.setListeners(pointListener, locationListener, routeListener);
                trackStore = announcerService.getTrackStore();
            }
            @Override public void onServiceDisconnected(ComponentName name) {
                announcerService.setListeners(null, null, null);
                trackStore = null;
            }
        };
        bindService(new Intent(this, AnnouncerService.class), serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(serviceConnection);
    }
}

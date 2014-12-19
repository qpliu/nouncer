package com.yrek.nouncer;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.yrek.nouncer.data.Location;
import com.yrek.nouncer.data.Point;
import com.yrek.nouncer.data.Route;
import com.yrek.nouncer.data.TrackPoint;
import com.yrek.nouncer.processor.PointProcessor;
import com.yrek.nouncer.processor.PointReceiver;
import com.yrek.nouncer.processor.RouteProcessor;

public class Main extends Activity {
    private RouteProcessor.Listener routeListener;
    private PointProcessor.Listener locationListener;
    private PointReceiver pointListener;
    private AnnouncerServiceConnection serviceConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        final ArrayAdapter<TrackPoint> listAdapter = new ArrayAdapter<TrackPoint>(this, R.layout.track_entry) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = Main.this.getLayoutInflater().inflate(R.layout.track_entry, parent, false);
                }
                TrackPoint trackPoint = getItem(position);
                ((TextView) convertView.findViewById(R.id.location_text)).setText(trackPoint.getLocation().getName());
                ((TextView) convertView.findViewById(R.id.entry_time)).setText(String.format("%tT", trackPoint.getEntryTime()));
                ((TextView) convertView.findViewById(R.id.exit_time)).setText(String.format("%tT", trackPoint.getExitTime()));
                long dt = trackPoint.getExitTime() - trackPoint.getEntryTime();
                if (dt <= 0) {
                    ((TextView) convertView.findViewById(R.id.time_stopped)).setText("");
                } else {
                    ((TextView) convertView.findViewById(R.id.time_differential)).setText(String.format("% 3d:%02d", dt / 60000L, dt % 60000L / 1000L));
                }
                if (position == 0) {
                    ((TextView) convertView.findViewById(R.id.time_differential)).setText("");
                } else {
                    TrackPoint lastPoint = getItem(position - 1);
                    dt = lastPoint.getExitTime() - trackPoint.getEntryTime();
                    if (dt <= 0) {
                        ((TextView) convertView.findViewById(R.id.time_differential)).setText("");
                    } else {
                        ((TextView) convertView.findViewById(R.id.time_differential)).setText(String.format("% 3d:%02d", dt / 60000L, dt % 60000L / 1000L));
                    }
                }
                return convertView;
            }
        };
        ((ListView) findViewById(R.id.track_list)).setAdapter(listAdapter);

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
                        listAdapter.clear();
                        listAdapter.addAll(serviceConnection.announcerService.getTrackStore().getTrackPoints(timestamp - 1080000L, timestamp));
                    }
                });
            }
            @Override public void receiveExit(final Location location, final long timestamp) {
                locationText.post(new Runnable() {
                    @Override public void run() {
                        locationText.setText(String.format("Exit: Location: %s %tR", location.getName(), timestamp));
                        listAdapter.clear();
                        listAdapter.addAll(serviceConnection.announcerService.getTrackStore().getTrackPoints(timestamp - 1080000L, timestamp));
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

        findViewById(R.id.start_button).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startService(new Intent(Main.this, AnnouncerService.class));
            }
        });
        findViewById(R.id.stop_button).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                serviceConnection.announcerService.stop();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        serviceConnection = new AnnouncerServiceConnection();
        bindService(new Intent(this, AnnouncerService.class), serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(serviceConnection);
    }

    private class AnnouncerServiceConnection implements ServiceConnection {
        AnnouncerService announcerService = null;

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            announcerService = ((AnnouncerService.LocalBinder) service).getService();
            announcerService.setListeners(pointListener, locationListener, routeListener);
            findViewById(R.id.track_list).post(new Runnable() {
                @Override public void run() {
                    long timestamp = System.currentTimeMillis();
                    ArrayAdapter listAdapter = (ArrayAdapter) ((ListView) findViewById(R.id.track_list)).getAdapter();
                    listAdapter.clear();
                    listAdapter.addAll(announcerService.getTrackStore().getTrackPoints(timestamp - 1080000L, timestamp));
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            announcerService.setListeners(null, null, null);
            announcerService = null;
        }
    }
}

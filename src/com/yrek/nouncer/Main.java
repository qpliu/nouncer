package com.yrek.nouncer;

import java.util.Collections;
import java.util.List;

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
    private static final long MAX_AGE = 12L*3600L*1000L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        final ArrayAdapter<ListEntry> listAdapter = new ArrayAdapter<ListEntry>(this, R.layout.track_entry) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = Main.this.getLayoutInflater().inflate(R.layout.track_entry, parent, false);
                }
                getItem(position).display(convertView, this, position);
                return convertView;
            }
        };
        ((ListView) findViewById(R.id.track_list)).setAdapter(listAdapter);

        final TextView routeText = (TextView) findViewById(R.id.route_text);
        routeListener = new RouteProcessor.Listener() {
            @Override public void receiveEntry(final Route route, final long startTime, final int routeIndex, final long entryTime) {
                routeText.post(new Runnable() {
                    @Override public void run() {
                        routeText.setText(String.format("Entry: Route: %s Location %d: %s %tR %d:%02d", route.getName(), routeIndex, route.getRoutePoint(routeIndex).getLocation().getName(), entryTime, (entryTime - startTime) / 60000L, (entryTime - startTime) % 60000L / 1000L));
                    }
                });
            }
            @Override public void receiveExit(final Route route, final long startTime, final int routeIndex, final long exitTime) {
                routeText.post(new Runnable() {
                    @Override public void run() {
                        routeText.setText(String.format("Exit: Route: %s Location %d: %s %tR %d:%02d", route.getName(), routeIndex, route.getRoutePoint(routeIndex).getLocation().getName(), exitTime, (exitTime - startTime) / 60000L, (exitTime - startTime) % 60000L / 1000L));
                    }
                });
            }
        };

        final TextView locationText = (TextView) findViewById(R.id.location_text);
        locationListener = new PointProcessor.Listener() {
            @Override public void receiveEntry(final Location location, final long entryTime, final long timestamp) {
                locationText.post(new Runnable() {
                    @Override public void run() {
                        locationText.setText(String.format("Entry: Location: %s %tR", location.getName(), entryTime));
                        fillList(Math.max(timestamp, entryTime), serviceConnection.announcerService, listAdapter);
                    }
                });
            }
            @Override public void receiveExit(final Location location, final long exitTime, final long timestamp) {
                locationText.post(new Runnable() {
                    @Override public void run() {
                        locationText.setText(String.format("Exit: Location: %s %tR", location.getName(), exitTime));
                        fillList(Math.max(timestamp, exitTime), serviceConnection.announcerService, listAdapter);
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
                    ArrayAdapter<ListEntry> listAdapter = (ArrayAdapter<ListEntry>) ((ListView) findViewById(R.id.track_list)).getAdapter();
                    fillList(System.currentTimeMillis(), announcerService, listAdapter);
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            announcerService.setListeners(null, null, null);
            announcerService = null;
        }
    }

    private void fillList(long timestamp, AnnouncerService announcerService, final ArrayAdapter<ListEntry> listAdapter) {
        listAdapter.clear();
        RouteProcessor routeProcessor = new RouteProcessor(announcerService.getRouteStore(), null, new RouteProcessor.Listener() {
            @Override public void receiveEntry(Route route, long startTime, int routeIndex, long entryTime) {
                if (routeIndex + 1 >= route.getRoutePointCount()) {
                    listAdapter.insert(new ListEntry(route, startTime, entryTime), 0);
                }
            }
            @Override public void receiveExit(Route route, long startTime, int routeIndex, long exitTime) {
            }
        });
        List<TrackPoint> trackPoints = announcerService.getTrackStore().getTrackPoints(timestamp - MAX_AGE, timestamp, 50);
        Collections.reverse(trackPoints);
        for (TrackPoint trackPoint : trackPoints) {
            listAdapter.insert(new ListEntry(trackPoint), 0);
            routeProcessor.receiveEntry(trackPoint.getLocation(), trackPoint.getEntryTime(), trackPoint.getEntryTime());
            routeProcessor.receiveExit(trackPoint.getLocation(), trackPoint.getExitTime(), trackPoint.getExitTime());
        }
    }

    private class ListEntry {
        private final TrackPoint trackPoint;
        private final Route route;
        private final long routeStartTime;
        private final long routeEndTime;

        ListEntry(TrackPoint trackPoint) {
            this.trackPoint = trackPoint;
            this.route = null;
            this.routeStartTime = 0L;
            this.routeEndTime = 0L;
        }

        ListEntry(Route route, long routeStartTime, long routeEndTime) {
            this.trackPoint = null;
            this.route = route;
            this.routeStartTime = routeStartTime;
            this.routeEndTime = routeEndTime;
        }

        void display(View view, ArrayAdapter<ListEntry> listAdapter, int position) {
            if (trackPoint != null) {
                view.findViewById(R.id.location).setVisibility(View.VISIBLE);
                view.findViewById(R.id.route).setVisibility(View.GONE);
                ((TextView) view.findViewById(R.id.name)).setText(trackPoint.getLocation().getName());
                ((TextView) view.findViewById(R.id.entry_time)).setText(String.format("%tT", trackPoint.getEntryTime()));
                ((TextView) view.findViewById(R.id.exit_time)).setText(String.format("%tT", trackPoint.getExitTime()));
                long dt = Math.max(0L, trackPoint.getExitTime() - trackPoint.getEntryTime());
                ((TextView) view.findViewById(R.id.time_stopped)).setText(String.format("% 3d:%02d", dt / 60000L, dt % 60000L / 1000L));
                TrackPoint lastPoint = null;
                for (int i = 1; lastPoint == null && position + i < listAdapter.getCount(); i++) {
                    lastPoint = listAdapter.getItem(position + i).trackPoint;
                }
                if (lastPoint == null) {
                    ((TextView) view.findViewById(R.id.time_differential)).setText("");
                } else {
                    dt = Math.max(0L, trackPoint.getEntryTime() - lastPoint.getExitTime());
                    ((TextView) view.findViewById(R.id.time_differential)).setText(String.format("% 3d:%02d", dt / 60000L, dt % 60000L / 1000L));
                }
            } else {
                view.findViewById(R.id.location).setVisibility(View.GONE);
                view.findViewById(R.id.route).setVisibility(View.VISIBLE);
                ((TextView) view.findViewById(R.id.name)).setText(route.getName());
                ((TextView) view.findViewById(R.id.route_start_time)).setText(String.format("%tT", routeStartTime));
                ((TextView) view.findViewById(R.id.route_end_time)).setText(String.format("%tT", routeEndTime));
                long dt = Math.max(0L, routeEndTime - routeStartTime);
                ((TextView) view.findViewById(R.id.route_time)).setText(String.format("%d:%02d", dt / 60000L, dt % 60000L / 1000L));
            }
        }
    }
}

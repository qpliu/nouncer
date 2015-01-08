package com.yrek.nouncer;

import java.util.Collections;
import java.util.List;

import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.yrek.nouncer.data.Location;
import com.yrek.nouncer.data.Point;
import com.yrek.nouncer.data.Route;
import com.yrek.nouncer.data.TrackPoint;
import com.yrek.nouncer.processor.RouteProcessor;

class TrackListWidget extends Widget {
    private AnnouncerService announcerService = null;
    private final ArrayAdapter<ListEntry> listAdapter;
    private static final long MAX_AGE = 12L*3600L*1000L;

    TrackListWidget(final Main activity, int id) {
        super(activity, id);
        this.listAdapter = new ArrayAdapter<ListEntry>(activity, R.layout.track_list_entry) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = activity.getLayoutInflater().inflate(R.layout.track_list_entry, parent, false);
                }
                getItem(position).display(convertView, this, position);
                return convertView;
            }
        };

        ((ListView) view).setAdapter(listAdapter);
        ((ListView) view).setOnItemClickListener(listItemClickListener);
    }

    @Override
    public void receiveEntry(final Location location, final long entryTime, final double entryHeading, final double entrySpeed, final long timestamp) {
        post(new Runnable() {
            @Override public void run() {
                fillList(Math.max(timestamp, entryTime));
            }
        });
    }

    @Override
    public void receiveExit(final Location location, final long exitTime, final double exitHeading, final double exitSpeed, final long timestamp) {
        post(new Runnable() {
            @Override public void run() {
                fillList(Math.max(timestamp, exitTime));
            }
        });
    }

    @Override
    public void onServiceConnected(AnnouncerService announcerService) {
        this.announcerService = announcerService;
        post(new Runnable() {
            @Override public void run() {
                fillList(System.currentTimeMillis());
            }
        });
    }

    @Override
    public void onServiceDisconnected() {
        this.announcerService = null;
    }

    @Override
    public void onShow() {
        fillList(System.currentTimeMillis());
    }

    private void fillList(long timestamp) {
        listAdapter.clear();
        RouteProcessor routeProcessor = new RouteProcessor(announcerService.getStore().getRouteStore(), null, announcerService.getStore().getAvailabilityStore(), new RouteProcessor.Listener() {
            @Override public void receiveEntry(Route route, long startTime, int routeIndex, long entryTime, double entryHeading, double entrySpeed) {
                if (route.isStarred() && routeIndex + 1 >= route.getRoutePointCount()) {
                    listAdapter.insert(new ListEntry(route, startTime, entryTime), 0);
                }
            }
            @Override public void receiveExit(Route route, long startTime, int routeIndex, long exitTime, double exitHeading, double exitSpeed) {
            }
        });
        List<TrackPoint> trackPoints = announcerService.getStore().getTrackStore().getTrackPoints(timestamp - MAX_AGE, timestamp, 50);
        Collections.reverse(trackPoints);
        for (TrackPoint trackPoint : trackPoints) {
            if (trackPoint.getLocation().isHidden()) {
                continue;
            }
            listAdapter.insert(new ListEntry(trackPoint), 0);
            routeProcessor.receiveEntry(trackPoint.getLocation(), trackPoint.getEntryTime(), trackPoint.getEntryHeading(), trackPoint.getEntrySpeed(), trackPoint.getEntryTime());
            routeProcessor.receiveExit(trackPoint.getLocation(), trackPoint.getExitTime(), trackPoint.getExitHeading(), trackPoint.getExitSpeed(), trackPoint.getExitTime());
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
                ((TextView) view.findViewById(R.id.entry_heading)).setText(headingName(trackPoint.getEntryHeading()));
                ((TextView) view.findViewById(R.id.exit_heading)).setText(headingName(trackPoint.getExitHeading()));
                ((TextView) view.findViewById(R.id.entry_speed)).setText(String.format("%.1f", trackPoint.getEntrySpeed()*2.23694));
                ((TextView) view.findViewById(R.id.exit_speed)).setText(String.format("%.1f", trackPoint.getExitSpeed()*2.23694));
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

        void onClick() {
            if (route != null) {
                activity.routeWidget.show(route);
            } else {
                activity.locationWidget.show(trackPoint.getLocation());
            }
        }
    }

    private final AdapterView.OnItemClickListener listItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            listAdapter.getItem(position).onClick();
        }
    };

    static String headingName(double heading) {
        if (heading < -157.5 || heading > 157.5) {
            return "S";
        } else if (heading < -112.5) {
            return "SW";
        } else if  (heading < -65.5) {
            return "W";
        } else if  (heading < -22.5) {
            return "NW";
        } else if  (heading < 22.5) {
            return "N";
        } else if  (heading < 67.5) {
            return "NE";
        } else if  (heading < 112.5) {
            return "E";
        } else {
            return "SE";
        }
    }
}

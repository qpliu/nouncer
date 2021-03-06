package com.yrek.nouncer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.yrek.nouncer.data.Route;
import com.yrek.nouncer.data.RoutePoint;
import com.yrek.nouncer.data.TrackPoint;
import com.yrek.nouncer.processor.RouteProcessor;

class RouteWidget extends Widget {
    private Route route;
    private final ArrayAdapter<RoutePointEntry> routePointAdapter;
    private final ArrayAdapter<TrackEntry> trackAdapter;
    private static final long MAX_AGE = 10L*24L*3600L*1000L;

    RouteWidget(final Main activity, int id) {
        super(activity, id);
        this.routePointAdapter = new ArrayAdapter<RoutePointEntry>(activity, R.layout.route_point_list_entry) {
            @Override public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = activity.getLayoutInflater().inflate(R.layout.route_point_list_entry, parent, false);
                }
                getItem(position).display(convertView);
                return convertView;
            }
        };
        ((ListView) view.findViewById(R.id.route_point_list)).setAdapter(routePointAdapter);
        ((ListView) view.findViewById(R.id.route_point_list)).setOnItemClickListener(routePointEntryClickListener);
        this.trackAdapter = new ArrayAdapter<TrackEntry>(activity, R.layout.track_list_entry) {
            @Override public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = activity.getLayoutInflater().inflate(R.layout.track_list_entry, parent, false);
                }
                getItem(position).display(convertView, this, position);
                return convertView;
            }
        };
        ((ListView) view.findViewById(R.id.track_list)).setAdapter(trackAdapter);
        ((ListView) view.findViewById(R.id.track_list)).setOnItemClickListener(trackEntryClickListener);

        view.findViewById(R.id.delete_button).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                new AlertDialog.Builder(activity).setTitle(String.format("Delete route: %s", route.getName())).setNegativeButton("Cancel", null).setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        activity.store.getRouteStore().delete(route);
                        activity.show(activity.tabsWidget, activity.routeListWidget);
                        activity.stravaWidget.invalidateCache();
                    }
                }).create().show();
            }
        });
        TextChanger.setup((TextView) view.findViewById(R.id.name), (EditText) view.findViewById(R.id.edit_name), new TextChanger.OnTextChanged() {
            @Override public void onTextChanged(String text) {
                route.setName(text);
            }
        });
    }

    private void renderEntry(View view, RoutePoint item) {
        ((TextView) view.findViewById(R.id.name)).setText(item.getLocation().getName());
    }

    private class RoutePointEntry {
        private final RoutePoint routePoint;
        private final boolean entryAnnouncement;
        private final boolean exitAnnouncement;
        private final boolean marginalCheckbox;

        RoutePointEntry(RoutePoint routePoint, boolean entryAnnouncement, boolean exitAnnouncement, boolean marginalCheckbox) {
            this.routePoint = routePoint;
            this.entryAnnouncement = entryAnnouncement;
            this.exitAnnouncement = exitAnnouncement;
            this.marginalCheckbox = marginalCheckbox;
        }

        boolean isName() {
            return !entryAnnouncement && !exitAnnouncement && !marginalCheckbox;
        }

        void display(View view) {
            if (!entryAnnouncement && !exitAnnouncement && !marginalCheckbox) {
                view.findViewById(R.id.name).setVisibility(View.VISIBLE);
                view.findViewById(R.id.entry_label).setVisibility(View.GONE);
                view.findViewById(R.id.exit_label).setVisibility(View.GONE);
                view.findViewById(R.id.announcement_spinner).setVisibility(View.GONE);
                view.findViewById(R.id.marginal_label).setVisibility(View.GONE);
                view.findViewById(R.id.marginal_checkbox).setVisibility(View.GONE);
                ((TextView) view.findViewById(R.id.name)).setText(routePoint.getLocation().getName());
                return;
            }
            view.findViewById(R.id.name).setVisibility(View.GONE);
            if (marginalCheckbox) {
                view.findViewById(R.id.entry_label).setVisibility(View.GONE);
                view.findViewById(R.id.exit_label).setVisibility(View.GONE);
                view.findViewById(R.id.announcement_spinner).setVisibility(View.GONE);
                view.findViewById(R.id.marginal_label).setVisibility(View.VISIBLE);
                view.findViewById(R.id.marginal_checkbox).setVisibility(View.VISIBLE);
                final CheckBox checkBox = (CheckBox) view.findViewById(R.id.marginal_checkbox);
                checkBox.setChecked(routePoint.isMarginal());
                checkBox.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        routePoint.setMarginal(checkBox.isChecked());
                    }
                });
                return;
            }
            view.findViewById(R.id.marginal_label).setVisibility(View.GONE);
            view.findViewById(R.id.marginal_checkbox).setVisibility(View.GONE);
            if (entryAnnouncement) {
                view.findViewById(R.id.entry_label).setVisibility(View.VISIBLE);
                view.findViewById(R.id.exit_label).setVisibility(View.GONE);
            } else {
                view.findViewById(R.id.entry_label).setVisibility(View.GONE);
                view.findViewById(R.id.exit_label).setVisibility(View.VISIBLE);
            }
            view.findViewById(R.id.announcement_spinner).setVisibility(View.VISIBLE);
            final ArrayAdapter<Announcements.Announcement> adapter = new ArrayAdapter<Announcements.Announcement>(activity, R.layout.announcement_spinner_entry) {
                @Override public View getDropDownView(int position, View convertView, ViewGroup parent) {
                    return getView(position, convertView, parent);
                }
                @Override public View getView(int position, View convertView, ViewGroup parent) {
                    if (convertView == null) {
                        convertView = activity.getLayoutInflater().inflate(R.layout.announcement_spinner_entry, parent, false);
                    }
                    Announcements.Announcement a = getItem(position);
                    String announcement = entryAnnouncement ? routePoint.getEntryAnnouncement() : routePoint.getExitAnnouncement();
                    ((TextView) convertView).setText(a != null ? a.name : String.format("Custom (%.5s%s)", announcement, announcement != null && announcement.length() > 5 ? "..." : ""));
                    return convertView;
                }
            };
            adapter.addAll(activity.announcements.getAnnouncements());
            final Spinner spinner = (Spinner) view.findViewById(R.id.announcement_spinner);
            spinner.setAdapter(adapter);
            Announcements.Announcement a = activity.announcements.getByAnnouncement(entryAnnouncement ? routePoint.getEntryAnnouncement() : routePoint.getExitAnnouncement());
            int spinnerSelection = 0;
            if (a != null) {
                for (int i = 0; i < adapter.getCount(); i++) {
                    if (a == adapter.getItem(i)) {
                        spinnerSelection = i;
                        break;
                    }
                }
            }
            if (a != null && a.custom) {
                adapter.insert(null, spinnerSelection);
            }
            spinner.setOnItemSelectedListener(null);
            spinner.setSelection(spinnerSelection, false);
            final int finalSpinnerSelection = spinnerSelection;
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                private int oldSelection = finalSpinnerSelection;
                @Override public void onItemSelected(AdapterView<?> parent, View v, final int position, long id) {
                    Announcements.Announcement a = adapter.getItem(position);
                    if (a == null) {
                        assert false;
                    } else if (a.custom) {
                        final AdapterView.OnItemSelectedListener onItemSelectedListener = this;
                        final View dialogView = activity.getLayoutInflater().inflate(R.layout.custom_announcement_dialog, null);
                        String announcement = entryAnnouncement ? routePoint.getEntryAnnouncement() : routePoint.getExitAnnouncement();
                        ((TextView) dialogView).setText(announcement == null ? "" : announcement);
                        new AlertDialog.Builder(activity).setView(dialogView).setTitle(String.format("Set custom %s announcement", entryAnnouncement ? "entry" : "exit")).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface dialog, int which) {
                                spinner.setOnItemSelectedListener(null);
                                spinner.setSelection(oldSelection, false);
                                spinner.setOnItemSelectedListener(onItemSelectedListener);
                            }
                        }).setPositiveButton("Set announcement", new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface dialog, int which) {
                                if (entryAnnouncement) {
                                    routePoint.setEntryAnnouncement(((TextView) dialogView).getText().toString());
                                } else {
                                    routePoint.setExitAnnouncement(((TextView) dialogView).getText().toString());
                                }
                                spinner.setOnItemSelectedListener(null);
                                if (position > 0 && adapter.getItem(position - 1) == null) {
                                    spinner.setSelection(position - 1, false);
                                } else {
                                    adapter.insert(null, position);
                                    spinner.setSelection(position, false);
                                }
                                spinner.setOnItemSelectedListener(onItemSelectedListener);
                                oldSelection = spinner.getSelectedItemPosition();
                            }
                        }).create().show();
                    } else if (entryAnnouncement) {
                        routePoint.setEntryAnnouncement(a.announcement);
                        adapter.remove(null);
                        oldSelection = spinner.getSelectedItemPosition();
                    } else {
                        routePoint.setExitAnnouncement(a.announcement);
                        adapter.remove(null);
                        oldSelection = spinner.getSelectedItemPosition();
                    }
                }
                @Override public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }
    }

    private final AdapterView.OnItemClickListener routePointEntryClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            while (position > 0 && !routePointAdapter.getItem(position).isName()) {
                position--;
            }
            RoutePointEntry item = routePointAdapter.getItem(position);
            if (position + 1 >= routePointAdapter.getCount() || routePointAdapter.getItem(position + 1).isName()) {
                routePointAdapter.insert(new RoutePointEntry(item.routePoint, true, false, false), position + 1);
                routePointAdapter.insert(new RoutePointEntry(item.routePoint, false, true, false), position + 2);
                routePointAdapter.insert(new RoutePointEntry(item.routePoint, false, false, true), position + 3);
            } else {
                while (position + 1 < routePointAdapter.getCount() && !routePointAdapter.getItem(position + 1).isName()) {
                    routePointAdapter.remove(routePointAdapter.getItem(position + 1));
                }
            }
        }
    };

    private class TrackEntry {
        private final TrackPoint trackPoint;
        private final Route route;
        private final ArrayList<TrackPoint> trackPoints;
        private final long routeStartTime;
        private final long routeEndTime;

        TrackEntry(TrackPoint trackPoint) {
            this.trackPoint = trackPoint;
            this.route = null;
            this.trackPoints = null;
            this.routeStartTime = 0L;
            this.routeEndTime = 0L;
        }

        TrackEntry(Route route, ArrayList<TrackPoint> trackPoints, long routeStartTime, long routeEndTime) {
            this.trackPoint = null;
            this.route = route;
            this.trackPoints = trackPoints;
            this.routeStartTime = routeStartTime;
            this.routeEndTime = routeEndTime;
        }

        void display(View view, ArrayAdapter<TrackEntry> adapter, int position) {
            if (trackPoint != null) {
                view.findViewById(R.id.name).setVisibility(View.VISIBLE);
                view.findViewById(R.id.location).setVisibility(View.VISIBLE);
                view.findViewById(R.id.route).setVisibility(View.GONE);
                ((TextView) view.findViewById(R.id.name)).setText(trackPoint.getLocation().getName());
                ((TextView) view.findViewById(R.id.entry_time)).setText(String.format("%tT", trackPoint.getEntryTime()));
                ((TextView) view.findViewById(R.id.exit_time)).setText(String.format("%tT", trackPoint.getExitTime()));
                long dt = Math.max(0L, trackPoint.getExitTime() - trackPoint.getEntryTime());
                ((TextView) view.findViewById(R.id.time_stopped)).setText(String.format("% 3d:%02d", dt / 60000L, dt % 60000L / 1000L));
                TrackPoint lastPoint = null;
                if (position + 1 < adapter.getCount()) {
                    lastPoint = adapter.getItem(position + 1).trackPoint;
                }
                if (lastPoint == null) {
                    ((TextView) view.findViewById(R.id.time_differential)).setText("");
                } else {
                    dt = Math.max(0L, trackPoint.getEntryTime() - lastPoint.getExitTime());
                    ((TextView) view.findViewById(R.id.time_differential)).setText(String.format("% 3d:%02d", dt / 60000L, dt % 60000L / 1000L));
                }
                ((TextView) view.findViewById(R.id.entry_heading)).setText("");
                ((TextView) view.findViewById(R.id.exit_heading)).setText("");
                ((TextView) view.findViewById(R.id.entry_speed)).setText(String.format("%.1f", trackPoint.getEntrySpeed()*2.23694));
                ((TextView) view.findViewById(R.id.exit_speed)).setText(String.format("%.1f", trackPoint.getExitSpeed()*2.23694));
            } else {
                view.findViewById(R.id.name).setVisibility(View.GONE);
                view.findViewById(R.id.location).setVisibility(View.GONE);
                view.findViewById(R.id.route).setVisibility(View.VISIBLE);
                ((TextView) view.findViewById(R.id.route_start_time)).setText(String.format("%1$te %1$tb %1$tT", routeStartTime));
                ((TextView) view.findViewById(R.id.route_end_time)).setText(String.format("%tT", routeEndTime));
                long dt = Math.max(0L, routeEndTime - routeStartTime);
                ((TextView) view.findViewById(R.id.route_time)).setText(String.format("%d:%02d", dt / 60000L, dt % 60000L / 1000L));
            }
        }
    }

    private final AdapterView.OnItemClickListener trackEntryClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            while (position > 0 && trackAdapter.getItem(position).trackPoint != null) {
                position--;
            }
            if (position + 1 >= trackAdapter.getCount() || trackAdapter.getItem(position + 1).trackPoint == null) {
                for (TrackPoint trackPoint : trackAdapter.getItem(position).trackPoints) {
                    trackAdapter.insert(new TrackEntry(trackPoint), position + 1);
                }
            } else {
                while (position + 1 < trackAdapter.getCount() && trackAdapter.getItem(position + 1).trackPoint != null) {
                    trackAdapter.remove(trackAdapter.getItem(position + 1));
                }
            }
        }
    };

    public void show(final Route route) {
        activity.show(activity.tabsWidget, this);
        this.route = route;
        view.findViewById(R.id.name).setVisibility(View.VISIBLE);
        view.findViewById(R.id.edit_name).setVisibility(View.GONE);
        ((TextView) view.findViewById(R.id.name)).setText(route.getName());
        routePointAdapter.clear();
        for (RoutePoint routePoint : route.getRoutePoints()) {
            routePointAdapter.add(new RoutePointEntry(routePoint, false, false, false));
        }
        trackAdapter.clear();
        if (activity.store == null) {
            return;
        }
        activity.threadPool.execute(new Runnable() {
            @Override public void run() {
                final ArrayList<TrackEntry> trackEntries = new ArrayList<TrackEntry>();
                final ArrayList<TrackPoint> points = new ArrayList<TrackPoint>();
                RouteProcessor routeProcessor = new RouteProcessor(activity.store.getRouteStore(), null, activity.store.getAvailabilityStore(), new RouteProcessor.Listener() {
                    @Override public void receiveEntry(Route entryRoute, long startTime, int routeIndex, long entryTime, double entryHeading, double entrySpeed) {
                        if (route.equals(entryRoute) && routeIndex + 1 >= route.getRoutePointCount()) {
                            while (points.size() > 0 && !points.get(0).getLocation().equals(route.getRoutePoint(0).getLocation())) {
                                points.remove(0);
                            }
                            trackEntries.add(new TrackEntry(route, new ArrayList<TrackPoint>(points), startTime, entryTime));
                        }
                    }
                    @Override public void receiveExit(Route route, long startTime, int routeIndex, long exitTime, double exitHeading, double exitSpeed) {
                    }
                });
                List<TrackPoint> trackPoints = activity.store.getTrackStore().getTrackPoints(System.currentTimeMillis() - MAX_AGE, System.currentTimeMillis(), 500);
                Collections.reverse(trackPoints);
                for (TrackPoint trackPoint : trackPoints) {
                    points.add(trackPoint);
                    while (points.size() > route.getRoutePointCount()) {
                        points.remove(0);
                    }
                    routeProcessor.receiveEntry(trackPoint.getLocation(), trackPoint.getEntryTime(), trackPoint.getEntryHeading(), trackPoint.getEntrySpeed(), trackPoint.getEntryTime());
                    routeProcessor.receiveExit(trackPoint.getLocation(), trackPoint.getExitTime(), trackPoint.getExitHeading(), trackPoint.getExitSpeed(), trackPoint.getExitTime());
                }
                post(new Runnable() {
                    @Override public void run() {
                        for (TrackEntry trackEntry : trackEntries) {
                            trackAdapter.insert(trackEntry, 0);
                        }
                    }
                });
            }
        });
    }
}

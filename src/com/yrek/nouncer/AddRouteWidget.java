package com.yrek.nouncer;

import java.util.ArrayList;
import java.util.Comparator;

import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.yrek.nouncer.data.Location;
import com.yrek.nouncer.data.Route;
import com.yrek.nouncer.data.RoutePoint;
import com.yrek.nouncer.processor.PointProcessor;

class AddRouteWidget extends Widget {
    private OnFinish onFinish;
    private final ArrayAdapter<LocationHolder> locationListAdapter;
    private final EditText name;
    private boolean initialized = false;
    private Spinner footerSpinner = null;
    private ArrayAdapter<Location> footerSpinnerAdapter = null;

    AddRouteWidget(final Main activity, int id) {
        super(activity, id);
        name = (EditText) view.findViewById(R.id.name);
        view.findViewById(R.id.add_button).setOnClickListener(onClickListener);
        view.findViewById(R.id.cancel_button).setOnClickListener(onClickListener);

        locationListAdapter = new ArrayAdapter<LocationHolder>(activity, R.layout.add_route_location_entry) {
            @Override public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = activity.getLayoutInflater().inflate(R.layout.add_route_location_entry, parent, false);
                }
                return renderLocationEntry(convertView, getItem(position), position);
            }
        };
    }

    private static class LocationHolder {
        final Location location;

        LocationHolder(Location location) {
            this.location = location;
        }
    }

    private View renderLocationEntry(View v, final LocationHolder location, final int position) {
        if (position < 0) {
            v.findViewById(R.id.delete_entry_button).setEnabled(false);
        } else {
            v.findViewById(R.id.delete_entry_button).setEnabled(true);
            v.findViewById(R.id.delete_entry_button).setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    locationListAdapter.remove(location);
                    updateFooter();
                }
            });
        }
        v.findViewById(R.id.insert_entry_button).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (position >= 0) {
                    locationListAdapter.insert(new LocationHolder(null), position);
                } else {
                    locationListAdapter.add(new LocationHolder(null));
                }
                updateFooter();
            }
        });
        if (position < 1) {
            v.findViewById(R.id.up_entry_button).setEnabled(false);
        } else {
            v.findViewById(R.id.up_entry_button).setEnabled(true);
            v.findViewById(R.id.up_entry_button).setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    locationListAdapter.remove(location);
                    locationListAdapter.insert(location, position - 1);
                    updateFooter();
                }
            });
        }
        if (position < 0 || position + 1 >= locationListAdapter.getCount()) {
            v.findViewById(R.id.down_entry_button).setEnabled(false);
        } else {
            v.findViewById(R.id.down_entry_button).setEnabled(true);
            v.findViewById(R.id.down_entry_button).setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    locationListAdapter.remove(location);
                    locationListAdapter.insert(location, position + 1);
                    updateFooter();
                }
            });
        }

        final Spinner spinner = (Spinner) v.findViewById(R.id.location);
        final ArrayAdapter<Location> spinnerAdapter = new ArrayAdapter<Location>(activity, R.layout.add_route_location_spinner_entry) {
            @Override public View getDropDownView(int spinnerPosition, View convertView, ViewGroup parent) {
                return getView(spinnerPosition, convertView, parent);
            }
            @Override public View getView(int spinnerPosition, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = activity.getLayoutInflater().inflate(R.layout.add_route_location_spinner_entry, parent, false);
                }
                return renderSpinnerEntry((TextView) convertView, spinnerPosition, getItem(spinnerPosition));
            }
        };
        spinner.setAdapter(spinnerAdapter);
        spinner.setOnItemSelectedListener(null);
        spinnerAdapter.add(null); // blank
        spinnerAdapter.add(null); // new location...
        spinnerAdapter.addAll(activity.store.getLocationStore().getLocations(true));
        if (position < 0) {
            footerSpinnerAdapter = spinnerAdapter;
            footerSpinner = spinner;
        }

        Location previous = null;
        for (int i = position < 0 ? locationListAdapter.getCount() - 1 : position - 1; i >= 0 && previous == null; i--) {
            previous = locationListAdapter.getItem(i).location;
        }
        setupSpinner(spinner, location, spinnerAdapter, previous);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View v, int spinnerPosition, long id) {
                if (spinnerPosition == 1) {
                    addNewLocation(position);
                } else {
                    selectLocation(position, spinnerAdapter.getItem(spinnerPosition));
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        return v;
    }

    private View renderSpinnerEntry(TextView v, int spinnerPosition, Location location) {
        if (spinnerPosition != 1) {
            v.setText(location == null ? "" : location.getName());
        } else {
            v.setText("New Location...");
        }
        return v;
    }

    private void setupSpinner(Spinner spinner, LocationHolder location, ArrayAdapter<Location> spinnerAdapter, final Location previousLocation) {
        spinnerAdapter.sort(new Comparator<Location>() {
            @Override public int compare(Location lhs, Location rhs) {
                if (lhs == null) {
                    if (rhs == null) {
                        return 0;
                    }
                    return -1;
                } else if (rhs == null) {
                    return 1;
                }
                if (lhs.equals(rhs)) {
                    return 0;
                }
                if (previousLocation == null) {
                    return lhs.getName().compareTo(rhs.getName());
                }
                return Double.compare(PointProcessor.distance(previousLocation, lhs), PointProcessor.distance(previousLocation, rhs));
            }
        });
        int selection = 0;
        if (location != null && location.location != null) {
            for (int i = 2; i < spinnerAdapter.getCount(); i++) {
                if (location.location.equals(spinnerAdapter.getItem(i))) {
                    selection = i;
                    break;
                }
            }
        }
        spinner.setSelection(selection, false);
    }

    private void addNewLocation(final int position) {
        final OnFinish saveOnFinish = onFinish;
        final ArrayList<LocationHolder> saveLocations = new ArrayList<LocationHolder>();
        for (int i = 0; i < locationListAdapter.getCount(); i++) {
            saveLocations.add(locationListAdapter.getItem(i));
        }
        final String saveName = name.getText().toString();
        activity.addLocationWidget.show(new AddLocationWidget.OnFinish() {
            @Override public void onFinish(Location newLocation) {
                show(saveOnFinish);
                name.setText(saveName);
                if (newLocation != null) {
                    if (position < 0) {
                        saveLocations.add(new LocationHolder(newLocation));
                    } else {
                        saveLocations.set(position, new LocationHolder(newLocation));
                    }
                }
                locationListAdapter.addAll(saveLocations);
                updateFooter();
            }
        });
    }

    private void selectLocation(int position, Location location) {
        if (position < 0) {
            if (location != null) {
                locationListAdapter.add(new LocationHolder(location));
                updateFooter();
            }
            return;
        }
        locationListAdapter.remove(locationListAdapter.getItem(position));
        locationListAdapter.insert(new LocationHolder(location), position);
        updateFooter();
    }

    private void updateFooter() {
        Location last = null;
        for (int i = locationListAdapter.getCount() - 1; i >= 0 && last == null; i--) {
            last = locationListAdapter.getItem(i).location;
        }
        setupSpinner(footerSpinner, null, footerSpinnerAdapter, last);
    }

    private final View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
            case R.id.add_button:
                add();
                break;
            case R.id.cancel_button:
                onFinish.onFinish(null);
                break;
            }
        }
    };

    private void add() {
        String name = ((EditText) view.findViewById(R.id.name)).getText().toString().trim();
        if (name.length() == 0) {
            activity.notificationWidget.show("Add Route: Empty name");
            return;
        }
        ArrayList<Location> locations = new ArrayList<Location>();
        for (int i = 0; i < locationListAdapter.getCount(); i++) {
            if (locationListAdapter.getItem(i).location != null) {
                locations.add(locationListAdapter.getItem(i).location);
            }
        }
        if (locations.size() == 0) {
            activity.notificationWidget.show("Add Route: No locations");
            return;
        }
        Route route = activity.store.getRouteStore().addRoute(name, locations);
        // set default announcements
        int count = route.getRoutePointCount();
        for (int i = 0; i < count; i++) {
            RoutePoint routePoint = route.getRoutePoint(i);
            Announcements.Announcement announcement = activity.announcements.getEntryDefault(i, count);
            if (announcement != null) {
                routePoint.setEntryAnnouncement(announcement.announcement);
            }
            announcement = activity.announcements.getExitDefault(i, count);
            if (announcement != null) {
                routePoint.setExitAnnouncement(announcement.announcement);
            }
        }
        onFinish.onFinish(route);
    }

    interface OnFinish {
        public void onFinish(Route route);
    }

    void show(OnFinish onFinish) {
        this.onFinish = onFinish;
        activity.show(activity.tabsWidget, activity.addRouteWidget);
        locationListAdapter.clear();
        name.setText("");
    }

    @Override
    public void onShow() {
        if (!initialized && activity.store != null) {
            ListView listView = (ListView) view.findViewById(R.id.locations);
            listView.addFooterView(renderLocationEntry(activity.getLayoutInflater().inflate(R.layout.add_route_location_entry, listView, false), null, -1));
            listView.setAdapter(locationListAdapter);
            initialized = true;
        }
    }
}

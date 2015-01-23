package com.yrek.nouncer;

import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.yrek.nouncer.data.Location;
import com.yrek.nouncer.processor.PointProcessor;

class AddLocationWidget extends Widget {
    private final ArrayAdapter<Location> nearbyLocations;
    private final double[] nearbyCenter = new double[2];
    private OnFinish onFinish;

    AddLocationWidget(final Main activity, int id) {
        super(activity, id);
        nearbyLocations = nearbyLocationsAdapter();
        view.findViewById(R.id.add_button).setOnClickListener(onClickListener);
        view.findViewById(R.id.cancel_button).setOnClickListener(onClickListener);
        ((AdapterView) view.findViewById(R.id.nearby_location_list)).setAdapter(nearbyLocations);
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
        String name = ((TextView) view.findViewById(R.id.name)).getText().toString().trim();
        if (name.length() == 0) {
            activity.notificationWidget.show("Add Location: Empty name");
            return;
        }
        double latitude;
        try {
            latitude = Double.parseDouble(((TextView) view.findViewById(R.id.latitude)).getText().toString().trim());
        } catch (NumberFormatException e) {
            activity.notificationWidget.show("Add Location: Invalid latitude");
            return;
        }
        if (latitude < -90.0 || latitude > 90.0) {
            activity.notificationWidget.show("Add Location: Invalid latitude");
            return;
        }
        double longitude;
        try {
            longitude = Double.parseDouble(((TextView) view.findViewById(R.id.longitude)).getText().toString().trim());
        } catch (NumberFormatException e) {
            activity.notificationWidget.show("Add Location: Invalid longitude");
            return;
        }
        if (longitude < -180.0 || longitude > 180.0) {
            activity.notificationWidget.show("Add Location: Invalid longitude");
            return;
        }
        double elevation = 0.0;
        try {
            elevation = Double.parseDouble(((TextView) view.findViewById(R.id.elevation)).getText().toString().trim())/3.28084;
        } catch (NumberFormatException e) {
        }
        onFinish.onFinish(activity.store.getLocationStore().addLocation(name, latitude, longitude, elevation));
    }

    interface OnFinish {
        public void onFinish(Location location);
    }

    void show(OnFinish onFinish) {
        this.onFinish = onFinish;
        activity.show(activity.tabsWidget, activity.addLocationWidget);
        ((TextView) view.findViewById(R.id.name)).setText("");
        ((TextView) view.findViewById(R.id.latitude)).setText("");
        view.findViewById(R.id.latitude).setEnabled(true);
        ((TextView) view.findViewById(R.id.longitude)).setText("");
        view.findViewById(R.id.longitude).setEnabled(true);
        ((TextView) view.findViewById(R.id.elevation)).setText("");
        view.findViewById(R.id.elevation).setEnabled(true);
        view.findViewById(R.id.nearby_locations).setVisibility(View.GONE);
        nearbyLocations.clear();
    }

    void show(OnFinish onFinish, String name, double latitude, double longitude, double elevation) {
        this.onFinish = onFinish;
        activity.show(activity.tabsWidget, activity.addLocationWidget);
        ((TextView) view.findViewById(R.id.name)).setText(name);
        ((TextView) view.findViewById(R.id.latitude)).setText(String.valueOf(latitude));
        view.findViewById(R.id.latitude).setEnabled(false);
        ((TextView) view.findViewById(R.id.longitude)).setText(String.valueOf(longitude));
        view.findViewById(R.id.longitude).setEnabled(false);
        ((TextView) view.findViewById(R.id.elevation)).setText(String.valueOf(elevation*3.28084));
        view.findViewById(R.id.elevation).setEnabled(false);
        nearbyCenter[0] = latitude;
        nearbyCenter[1] = longitude;
        nearbyLocations.clear();
        nearbyLocations.addAll(activity.store.getLocationStore().getLocations(latitude, longitude, 500.0));
        if (nearbyLocations.getCount() == 0) {
            view.findViewById(R.id.nearby_locations).setVisibility(View.GONE);
        } else {
            view.findViewById(R.id.nearby_locations).setVisibility(View.VISIBLE);
        }
    }

    private ArrayAdapter<Location> nearbyLocationsAdapter() {
        return new ArrayAdapter<Location>(activity, R.layout.nearby_location_list_entry) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = activity.getLayoutInflater().inflate(R.layout.nearby_location_list_entry, parent, false);
                }
                Location item = getItem(position);
                ((TextView) convertView.findViewById(R.id.name)).setText(item.getName());
                ((TextView) convertView.findViewById(R.id.distance)).setText(String.format("%.2fmi %s", PointProcessor.distance(nearbyCenter[0], nearbyCenter[1], item.getLatitude(), item.getLongitude())*0.000621371, TrackListWidget.headingName(PointProcessor.heading(nearbyCenter[0], nearbyCenter[1], item.getLatitude(), item.getLongitude()))));
                return convertView;
            }
        };
    }
}

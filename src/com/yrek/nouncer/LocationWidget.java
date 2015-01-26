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
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.yrek.nouncer.data.Location;
import com.yrek.nouncer.data.Route;

class LocationWidget extends Widget {
    private final ArrayAdapter<Route> routeAdapter;
    private Location location;

    LocationWidget(final Main activity, int id) {
        super(activity, id);
        this.routeAdapter = new ArrayAdapter<Route>(activity, R.layout.route_list_entry) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = activity.getLayoutInflater().inflate(R.layout.route_list_entry, parent, false);
                }
                renderEntry(convertView, getItem(position));
                return convertView;
            }
        };
        ((ListView) view.findViewById(R.id.route_list)).setAdapter(routeAdapter);
        ((ListView) view.findViewById(R.id.route_list)).setOnItemClickListener(itemClickListener);

        view.findViewById(R.id.delete_button).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                new AlertDialog.Builder(activity).setTitle(String.format("Delete location: %s", location.getName())).setNegativeButton("Cancel", null).setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        activity.store.getLocationStore().delete(location);
                        activity.show(activity.tabsWidget, activity.locationListWidget);
                        activity.stravaWidget.invalidateCache();
                    }
                }).create().show();
            }
        });
        TextChanger.setup((TextView) view.findViewById(R.id.name), (EditText) view.findViewById(R.id.edit_name), new TextChanger.OnTextChanged() {
            @Override public void onTextChanged(String text) {
                location.setName(text);
            }
        });
    }

    private void renderEntry(View view, Route item) {
        view.findViewById(R.id.starred_checkbox).setVisibility(View.GONE);
        ((TextView) view.findViewById(R.id.name)).setText(item.getName());
    }

    private final AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            activity.routeWidget.show(routeAdapter.getItem(position));
        }
    };

    public void show(Location location) {
        this.location = location;
        activity.show(activity.tabsWidget, this);
        view.findViewById(R.id.name).setVisibility(View.VISIBLE);
        view.findViewById(R.id.edit_name).setVisibility(View.GONE);
        ((TextView) view.findViewById(R.id.name)).setText(location.getName());
        ((TextView) view.findViewById(R.id.latitude)).setText(String.valueOf(location.getLatitude()));
        ((TextView) view.findViewById(R.id.longitude)).setText(String.valueOf(location.getLongitude()));
        ((TextView) view.findViewById(R.id.elevation)).setText(String.format("%.0fft", location.getElevation()*3.28084));
        routeAdapter.clear();
        if (activity.store != null) {
            routeAdapter.addAll(activity.store.getRouteStore().getRoutes(location));
        }
        view.findViewById(R.id.delete_button).setEnabled(routeAdapter.getCount() == 0);
    }
}

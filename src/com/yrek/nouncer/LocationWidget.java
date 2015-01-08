package com.yrek.nouncer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.yrek.nouncer.data.Location;
import com.yrek.nouncer.data.Route;
import com.yrek.nouncer.store.Store;

class LocationWidget extends Widget {
    private final ArrayAdapter<Route> routeAdapter;
    private Store store = null;
    private static final long MAX_AGE = 7L*24L*3600L*1000L;

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

    @Override
    public void onServiceConnected(AnnouncerService announcerService) {
        store = announcerService.getStore();
    }

    public void show(Location location) {
        activity.show(activity.tabsWidget, this);
        ((TextView) view.findViewById(R.id.name)).setText(location.getName());
        ((TextView) view.findViewById(R.id.latitude)).setText(String.valueOf(location.getLatitude()));
        ((TextView) view.findViewById(R.id.longitude)).setText(String.valueOf(location.getLongitude()));
        ((TextView) view.findViewById(R.id.elevation)).setText(String.format("%.0fft", location.getElevation()*3.28084));
        routeAdapter.clear();
        if (store != null) {
            routeAdapter.addAll(store.getRouteStore().getRoutes(location));
        }
    }
}

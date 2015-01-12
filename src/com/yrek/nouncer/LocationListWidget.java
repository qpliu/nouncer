package com.yrek.nouncer;

import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import com.yrek.nouncer.data.Location;

class LocationListWidget extends Widget {
    private final ArrayAdapter<Location> listAdapter;

    LocationListWidget(final Main activity, int id) {
        super(activity, id);
        this.listAdapter = new ArrayAdapter<Location>(activity, R.layout.location_list_entry) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = activity.getLayoutInflater().inflate(R.layout.location_list_entry, parent, false);
                }
                renderEntry(convertView, getItem(position));
                return convertView;
            }
        };

        ((ListView) view.findViewById(R.id.location_list)).setAdapter(listAdapter);
        ((ListView) view.findViewById(R.id.location_list)).setOnItemClickListener(listItemClickListener);
        view.findViewById(R.id.add_location_button).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                activity.addLocationWidget.show(new AddLocationWidget.OnFinish() {
                    @Override public void onFinish(Location location) {
                        if (location == null) {
                            activity.show(activity.tabsWidget, activity.locationListWidget);
                        } else {
                            activity.locationWidget.show(location);
                        }
                    }
                });
            }
        });
    }

    private final AdapterView.OnItemClickListener listItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            activity.locationWidget.show(listAdapter.getItem(position));
        }
    };

    private void renderEntry(View view, final Location item) {
        TextView textView = (TextView) view;
        textView.setText(item.getName());
        textView.setEnabled(!item.isHidden());
    }

    private final Runnable fillList = new Runnable() {
        @Override
        public void run() {
            if (activity.store != null) {
                listAdapter.clear();
                listAdapter.addAll(activity.store.getLocationStore().getLocations(true));
            }
        }
    };

    @Override
    public void onServiceConnected() {
        post(fillList);
    }

    @Override
    public void onShow() {
        fillList.run();
    }
}

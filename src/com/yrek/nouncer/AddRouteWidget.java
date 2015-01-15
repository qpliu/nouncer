package com.yrek.nouncer;

import java.util.ArrayList;

import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.EditText;

import com.yrek.nouncer.data.Location;
import com.yrek.nouncer.data.Route;

class AddRouteWidget extends Widget {
    private OnFinish onFinish;
    private final ArrayAdapter<Location> locationListAdapter;
    private final EditText name;

    AddRouteWidget(final Main activity, int id) {
        super(activity, id);
        name = (EditText) view.findViewById(R.id.name);
        view.findViewById(R.id.add_button).setOnClickListener(onClickListener);
        view.findViewById(R.id.cancel_button).setOnClickListener(onClickListener);

        locationListAdapter = new ArrayAdapter<Location>(activity, R.layout.add_route_location_entry) {
            @Override public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = activity.getLayoutInflater().inflate(R.layout.add_route_location_entry, parent, false);
                }
                return renderLocationEntry(convertView, getItem(position), position);
            }
        };
        ListView listView = (ListView) view.findViewById(R.id.locations);
        listView.addFooterView(renderLocationEntry(activity.getLayoutInflater().inflate(R.layout.add_route_location_entry, listView, false), null, -1));
        listView.setAdapter(locationListAdapter);
    }

    private View renderLocationEntry(View v, final Location location, final int position) {
        if (position < 0) {
            v.findViewById(R.id.delete_entry_button).setEnabled(false);
        } else {
            v.findViewById(R.id.delete_entry_button).setEnabled(true);
            v.findViewById(R.id.delete_entry_button).setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    locationListAdapter.remove(location);
                }
            });
        }
        v.findViewById(R.id.insert_entry_button).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (position >= 0) {
                    locationListAdapter.insert(null, position);
                } else {
                    locationListAdapter.add(null);
                }
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
                }
            });
        }
        //... set up location spinner
        return v;
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
            if (locationListAdapter.getItem(i) != null) {
                locations.add(locationListAdapter.getItem(i));
            }
        }
        if (locations.size() == 0) {
            activity.notificationWidget.show("Add Route: No locations");
            return;
        }
    }

    interface OnFinish {
        public void onFinish(Route route);
    }

    void show(OnFinish onFinish) {
        this.onFinish = onFinish;
        activity.show(activity.tabsWidget, activity.addRouteWidget);
        locationListAdapter.clear();
    }
}

package com.yrek.nouncer;

import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import com.yrek.nouncer.data.Location;
import com.yrek.nouncer.store.Store;

class LocationListWidget extends Widget {
    private final ArrayAdapter<Location> listAdapter;
    private Store store = null;

    LocationListWidget(final Main activity, int id) {
        super(activity, id);
        this.listAdapter = new ArrayAdapter<Location>(activity, R.layout.location_list_entry) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = activity.getLayoutInflater().inflate(R.layout.route_list_entry, parent, false);
                }
                renderEntry(convertView, getItem(position));
                return convertView;
            }
        };

        ((ListView) view.findViewById(R.id.location_list)).setAdapter(listAdapter);
        ((ListView) view.findViewById(R.id.location_list)).setOnItemClickListener(listItemClickListener);
        view.findViewById(R.id.add_location_button).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                activity.show(activity.tabsWidget, activity.addLocationWidget);
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
        TextView textView = (TextView) view.findViewById(R.id.name);
        textView.setText(item.getName());
        textView.setEnabled(!item.isHidden());
    }

    private void fillList() {
        if (store != null) {
            listAdapter.clear();
            listAdapter.addAll(store.getLocationStore().getLocations(true));
        }
    }

    @Override
    public void onServiceConnected(AnnouncerService announcerService) {
        store = announcerService.getStore();
        post(new Runnable() {
            @Override public void run() {
                fillList();
            }
        });
    }

    @Override
    public void onShow() {
        fillList();
    }
}

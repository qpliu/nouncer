package com.yrek.nouncer;

import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import com.yrek.nouncer.data.Route;
import com.yrek.nouncer.store.Store;

class RouteListWidget extends Widget {
    private final ArrayAdapter<Route> listAdapter;
    private Store store = null;

    RouteListWidget(final Main activity, int id) {
        super(activity, id);
        this.listAdapter = new ArrayAdapter<Route>(activity, R.layout.route_list_entry) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = activity.getLayoutInflater().inflate(R.layout.route_list_entry, parent, false);
                }
                renderEntry(convertView, getItem(position));
                return convertView;
            }
        };

        ((ListView) view.findViewById(R.id.route_list)).setAdapter(listAdapter);
        ((ListView) view.findViewById(R.id.route_list)).setOnItemClickListener(listItemClickListener);

        view.findViewById(R.id.add_route_button).setOnClickListener(buttonClickListener);
        view.findViewById(R.id.restrict_button).setOnClickListener(buttonClickListener);
        view.findViewById(R.id.unrestrict_button).setOnClickListener(buttonClickListener);
    }

    private final View.OnClickListener buttonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
            case R.id.add_route_button:
                activity.show(activity.tabsWidget, activity.addRouteWidget);
                break;
            case R.id.restrict_button:
                store.getRouteStore().hideNonstarred();
                fillList();
                break;
            case R.id.unrestrict_button:
                store.getRouteStore().unhideAll();
                fillList();
                break;
            default:
                break;
            }
        }
    };

    private final AdapterView.OnItemClickListener listItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            activity.routeWidget.show(listAdapter.getItem(position));
        }
    };

    private void renderEntry(View view, final Route item) {
        TextView textView = (TextView) view.findViewById(R.id.name);
        textView.setText(item.getName());
        textView.setEnabled(!item.isHidden());
        final CheckBox checkBox = (CheckBox) view.findViewById(R.id.starred_checkbox);
        checkBox.setChecked(item.isStarred());
        checkBox.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                item.setStarred(checkBox.isChecked());
            }
        });
    }

    private void fillList() {
        if (store != null) {
            listAdapter.clear();
            listAdapter.addAll(store.getRouteStore().getRoutes());
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

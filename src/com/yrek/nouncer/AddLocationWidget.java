package com.yrek.nouncer;

import android.view.View;
import android.widget.TextView;

import com.yrek.nouncer.data.Location;

class AddLocationWidget extends Widget {
    private OnFinish onFinish;

    AddLocationWidget(final Main activity, int id) {
        super(activity, id);
        view.findViewById(R.id.add_button).setOnClickListener(onClickListener);
        view.findViewById(R.id.cancel_button).setOnClickListener(onClickListener);
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
            activity.notificationWidget.show("Empty name");
            return;
        }
        double latitude;
        try {
            latitude = Double.parseDouble(((TextView) view.findViewById(R.id.latitude)).getText().toString().trim());
        } catch (NumberFormatException e) {
            activity.notificationWidget.show("Invalid latitude");
            return;
        }
        if (latitude < -90.0 || latitude > 90.0) {
            activity.notificationWidget.show("Invalid latitude");
            return;
        }
        double longitude;
        try {
            longitude = Double.parseDouble(((TextView) view.findViewById(R.id.longitude)).getText().toString().trim());
        } catch (NumberFormatException e) {
            activity.notificationWidget.show("Invalid longitude");
            return;
        }
        if (longitude < -180.0 || longitude > 180.0) {
            activity.notificationWidget.show("Invalid longitude");
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
    }
}

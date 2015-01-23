package com.yrek.nouncer;

import android.view.View;

class TabsWidget extends Widget {
    TabsWidget(final Main activity, int id) {
        super(activity, id);
        view.findViewById(R.id.home_button).setOnClickListener(onClickListener);
        view.findViewById(R.id.routes_button).setOnClickListener(onClickListener);
        view.findViewById(R.id.locations_button).setOnClickListener(onClickListener);
        view.findViewById(R.id.strava_button).setOnClickListener(onClickListener);
    }

    @Override
    public void onShow() {
        view.findViewById(R.id.strava_button).setVisibility(activity.stravaWidget.available() ? View.VISIBLE : View.GONE);
    }

    private final View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
            case R.id.home_button:
                activity.show(activity.tabsWidget, activity.startStopWidget, activity.statusWidget, activity.trackListWidget);
                break;
            case R.id.routes_button:
                activity.show(activity.tabsWidget, activity.routeListWidget);
                break;
            case R.id.locations_button:
                activity.show(activity.tabsWidget, activity.locationListWidget);
                break;
            case R.id.strava_button:
                activity.stravaWidget.enter();
                break;
            default:
                break;
            }
        }
    };
}

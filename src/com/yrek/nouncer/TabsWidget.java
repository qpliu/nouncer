package com.yrek.nouncer;

import android.view.View;

class TabsWidget extends Widget {
    TabsWidget(final Main activity, int id) {
        super(activity, id);
        view.findViewById(R.id.home).setOnClickListener(onClickListener);
        view.findViewById(R.id.routes).setOnClickListener(onClickListener);
        view.findViewById(R.id.locations).setOnClickListener(onClickListener);
    }

    private final View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
            case R.id.home:
                show(activity.tabsWidget, activity.startStopWidget, activity.statusWidget, activity.trackListWidget);
                break;
            case R.id.routes:
                show(activity.tabsWidget, activity.routeListWidget);
                break;
            default:
                break;
            }
        }
    };

    void show(Widget... widgets) {
        for (Widget w : activity.widgets) {
            w.hide();
        }
        for (Widget w : widgets) {
            w.show();
        }
    }
}

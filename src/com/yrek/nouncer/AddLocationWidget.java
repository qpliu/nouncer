package com.yrek.nouncer;

import android.view.View;

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
                activity.notificationWidget.show("Not implemented");
                break;
            case R.id.cancel_button:
                onFinish.onFinish(null);
                break;
            }
        }
    };

    interface OnFinish {
        public void onFinish(Location location);
    }

    void show(OnFinish onFinish) {
        this.onFinish = onFinish;
        activity.show(activity.tabsWidget, activity.addLocationWidget);
    }
}

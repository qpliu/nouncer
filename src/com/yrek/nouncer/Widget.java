package com.yrek.nouncer;

import android.view.View;

import com.yrek.nouncer.data.Location;
import com.yrek.nouncer.data.Point;
import com.yrek.nouncer.data.Route;
import com.yrek.nouncer.processor.PointProcessor;
import com.yrek.nouncer.processor.PointReceiver;
import com.yrek.nouncer.processor.RouteProcessor;

public abstract class Widget implements PointProcessor.Listener, PointReceiver, RouteProcessor.Listener {
    protected final Main activity;
    protected final View view;

    protected Widget(Main activity, int id) {
        this.activity = activity;
        this.view = activity.findViewById(id);
    }

    public void hide() {
        view.setVisibility(View.GONE);
        onHide();
    }

    public void show() {
        view.setVisibility(View.VISIBLE);
        onShow();
    }

    @Override
    public void receiveEntry(Location location, long entryTime, double entryHeading, double entrySpeed, long timestamp) {
    }

    @Override
    public void receiveExit(Location location, long exitTime, double exitHeading, double exitSpeed, long timestamp) {
    }

    @Override public void receivePoint(Point point) {
    }

    @Override
    public void receiveEntry(Route route, long startTime, int routeIndex, long entryTime, double entryHeading, double entrySpeed) {
    }

    @Override
    public void receiveExit(Route route, long startTime, int routeIndex, long exitTime, double exitHeading, double exitSpeed) {
    }

    public void onServiceConnected() {
    }

    public void onServiceDisconnected() {
    }

    public void onHide() {
    }

    public void onShow() {
    }

    protected void post(Runnable runnable) {
        view.post(runnable);
    }
}

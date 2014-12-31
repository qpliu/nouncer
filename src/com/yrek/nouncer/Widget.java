package com.yrek.nouncer;

import android.view.View;

import com.yrek.nouncer.data.Location;
import com.yrek.nouncer.data.Point;
import com.yrek.nouncer.data.Route;
import com.yrek.nouncer.processor.PointProcessor;
import com.yrek.nouncer.processor.PointReceiver;
import com.yrek.nouncer.processor.RouteProcessor;

public abstract class Widget implements PointProcessor.Listener, PointReceiver, RouteProcessor.Listener {
    private final View view;

    protected Widget(View view) {
        this.view = view;
    }

    public void hide() {
        view.setVisibility(View.GONE);
    }

    public void show() {
        view.setVisibility(View.VISIBLE);
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

    public void onServiceConnected(AnnouncerService announcerService) {
    }

    public void onServiceDisconnected() {
    }

    protected void post(Runnable runnable) {
        view.post(runnable);
    }
}

package com.yrek.nouncer;

import java.util.ArrayList;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import com.yrek.nouncer.data.Location;
import com.yrek.nouncer.data.Point;
import com.yrek.nouncer.data.Route;
import com.yrek.nouncer.data.TrackPoint;
import com.yrek.nouncer.processor.PointProcessor;
import com.yrek.nouncer.processor.PointReceiver;
import com.yrek.nouncer.processor.RouteProcessor;

public class Main extends Activity {
    private final ArrayList<Widget> widgets = new ArrayList<Widget>();
    private AnnouncerServiceConnection serviceConnection;
    TabsWidget tabsWidget;
    StartStopWidget startStopWidget;
    StatusWidget statusWidget;
    TrackListWidget trackListWidget;
    RouteListWidget routeListWidget;
    RouteWidget routeWidget;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        widgets.clear();
        this.tabsWidget = addWidget(new TabsWidget(this, R.id.tabs_widget));
        this.startStopWidget = addWidget(new StartStopWidget(this, R.id.start_stop_widget));
        this.statusWidget = addWidget(new StatusWidget(this, R.id.status_widget));
        this.trackListWidget = addWidget(new TrackListWidget(this, R.id.track_list_widget));
        this.routeListWidget = addWidget(new RouteListWidget(this, R.id.route_list_widget));
        this.routeWidget = addWidget(new RouteWidget(this, R.id.route_widget));
    }

    private <W extends Widget> W addWidget(W widget) {
        widgets.add(widget);
        return widget;
    }

    void show(Widget... widgets) {
        for (Widget w : this.widgets) {
            w.hide();
        }
        for (Widget w : widgets) {
            w.show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        serviceConnection = new AnnouncerServiceConnection();
        bindService(new Intent(this, AnnouncerService.class), serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(serviceConnection);
    }

    private final RouteProcessor.Listener routeListener = new RouteProcessor.Listener() {
        @Override public void receiveEntry(Route route, long startTime, int routeIndex, long entryTime, double entryHeading, double entrySpeed) {
            for (Widget widget : widgets) {
                widget.receiveEntry(route, startTime, routeIndex, entryTime, entryHeading, entrySpeed);
            }
        }

        @Override public void receiveExit(Route route, long startTime, int routeIndex, long exitTime, double exitHeading, double exitSpeed) {
            for (Widget widget : widgets) {
                widget.receiveExit(route, startTime, routeIndex, exitTime, exitHeading, exitSpeed);
            }
        }
    };

    private final PointProcessor.Listener locationListener = new PointProcessor.Listener() {
        @Override public void receiveEntry(Location location, long entryTime, double entryHeading, double entrySpeed, long timestamp) {
            for (Widget widget : widgets) {
                widget.receiveEntry(location, entryTime, entryHeading, entrySpeed, timestamp);
            }
        }

        @Override public void receiveExit(Location location, long exitTime, double exitHeading, double exitSpeed, long timestamp) {
            for (Widget widget : widgets) {
                widget.receiveExit(location, exitTime, exitHeading, exitSpeed, timestamp);
            }
        }
    };

    private final PointReceiver pointListener = new PointReceiver() {
        @Override public void receivePoint(Point point) {
            for (Widget widget : widgets) {
                widget.receivePoint(point);
            }
        }
    };

    private class AnnouncerServiceConnection implements ServiceConnection {
        AnnouncerService announcerService = null;

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            announcerService = ((AnnouncerService.LocalBinder) service).getService();
            announcerService.setListeners(pointListener, locationListener, routeListener);
            for (Widget widget : widgets) {
                widget.onServiceConnected(announcerService);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            announcerService.setListeners(null, null, null);
            announcerService = null;
            for (Widget widget : widgets) {
                widget.onServiceDisconnected();
            }
        }
    }
}

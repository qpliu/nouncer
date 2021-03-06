package com.yrek.nouncer;

import java.util.ArrayList;
import java.util.concurrent.ScheduledThreadPoolExecutor;

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
import com.yrek.nouncer.store.Store;

public class Main extends Activity {
    private final ArrayList<Widget> widgets = new ArrayList<Widget>();
    private AnnouncerServiceConnection serviceConnection;
    ScheduledThreadPoolExecutor threadPool;
    AnnouncerService announcerService;
    Store store;
    Announcements announcements;
    TabsWidget tabsWidget;
    StartStopWidget startStopWidget;
    StatusWidget statusWidget;
    TrackListWidget trackListWidget;
    RouteListWidget routeListWidget;
    RouteWidget routeWidget;
    AddRouteWidget addRouteWidget;
    LocationListWidget locationListWidget;
    LocationWidget locationWidget;
    AddLocationWidget addLocationWidget;
    NotificationWidget notificationWidget;
    StravaWidget stravaWidget;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        this.announcements = new Announcements(this);

        this.threadPool = new ScheduledThreadPoolExecutor(2);

        this.widgets.clear();
        this.tabsWidget = addWidget(new TabsWidget(this, R.id.tabs_widget));
        this.startStopWidget = addWidget(new StartStopWidget(this, R.id.start_stop_widget));
        this.statusWidget = addWidget(new StatusWidget(this, R.id.status_widget));
        this.trackListWidget = addWidget(new TrackListWidget(this, R.id.track_list_widget));
        this.routeListWidget = addWidget(new RouteListWidget(this, R.id.route_list_widget));
        this.routeWidget = addWidget(new RouteWidget(this, R.id.route_widget));
        this.addRouteWidget = addWidget(new AddRouteWidget(this, R.id.add_route_widget));
        this.locationListWidget = addWidget(new LocationListWidget(this, R.id.location_list_widget));
        this.locationWidget = addWidget(new LocationWidget(this, R.id.location_widget));
        this.addLocationWidget = addWidget(new AddLocationWidget(this, R.id.add_location_widget));
        this.notificationWidget = addWidget(new NotificationWidget(this, R.id.notification_widget));
        this.stravaWidget = addWidget(new StravaWidget(this, R.id.strava_widget));
        onNewIntent(getIntent());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        threadPool.shutdownNow();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_VIEW)) {
            for (Widget w : widgets) {
                w.onNewIntent(intent);
            }
        }
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
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            announcerService = ((AnnouncerService.LocalBinder) service).getService();
            announcerService.setListeners(pointListener, locationListener, routeListener);
            store = announcerService.getStore();
            for (Widget widget : widgets) {
                widget.onServiceConnected();
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

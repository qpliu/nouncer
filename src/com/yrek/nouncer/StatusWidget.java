package com.yrek.nouncer;

import android.view.View;
import android.widget.TextView;

import com.yrek.nouncer.data.Location;
import com.yrek.nouncer.data.Point;
import com.yrek.nouncer.data.Route;
import com.yrek.nouncer.data.TrackPoint;

class StatusWidget extends Widget {
    private final TextView routeText;
    private final TextView locationText;
    private final TextView pointText;

    StatusWidget(Main activity, int id) {
        super(activity, id);
        this.routeText = (TextView) view.findViewById(R.id.route_text);
        this.locationText = (TextView) view.findViewById(R.id.location_text);
        this.pointText = (TextView) view.findViewById(R.id.point_text);
        pointText.setText("Point: None");
    }

    @Override
    public void receiveEntry(final Route route, final long startTime, final int routeIndex, final long entryTime, final double entryHeading, final double entrySpeed) {
        post(new Runnable() {
            @Override public void run() {
                routeText.setText(String.format("Entry: Route: %s Location %d: %s %tR %d:%02d %s %.1fmph", route.getName(), routeIndex, route.getRoutePoint(routeIndex).getLocation().getName(), entryTime, (entryTime - startTime) / 60000L, (entryTime - startTime) % 60000L / 1000L, TrackListWidget.headingName(entryHeading), entrySpeed*2.23694));
            }
        });
    }

    @Override
    public void receiveExit(final Route route, final long startTime, final int routeIndex, final long exitTime, final double exitHeading, final double exitSpeed) {
        post(new Runnable() {
            @Override public void run() {
                routeText.setText(String.format("Exit: Route: %s Location %d: %s %tR %d:%02d %s %.1fmph", route.getName(), routeIndex, route.getRoutePoint(routeIndex).getLocation().getName(), exitTime, (exitTime - startTime) / 60000L, (exitTime - startTime) % 60000L / 1000L, TrackListWidget.headingName(exitHeading), exitSpeed*2.23694));
            }
        });
    }

    @Override
    public void receiveEntry(final Location location, final long entryTime, final double entryHeading, final double entrySpeed, final long timestamp) {
        post(new Runnable() {
            @Override public void run() {
                locationText.setText(String.format("Entry: Location: %s %tR %s %.1fmph", location.getName(), entryTime, TrackListWidget.headingName(entryHeading), entrySpeed*2.23694));
            }
        });
    }

    @Override
    public void receiveExit(final Location location, final long exitTime, final double exitHeading, final double exitSpeed, final long timestamp) {
        post(new Runnable() {
            @Override public void run() {
                locationText.setText(String.format("Exit: Location: %s %tR %s %.1fmph", location.getName(), exitTime, TrackListWidget.headingName(exitHeading), exitSpeed*2.23694));
            }
        });
    }

    @Override
    public void receivePoint(final Point point) {
        post(new Runnable() {
            @Override public void run() {
                pointText.setText(String.format("Point: %tT: %f,%f,%.1f", point.getTime(), point.getLatitude(), point.getLongitude(), point.getElevation()));
            }
        });
    }
}

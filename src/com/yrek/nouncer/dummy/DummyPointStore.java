package com.yrek.nouncer.dummy;

import java.util.ArrayList;
import java.util.List;

import com.yrek.nouncer.data.Point;
import com.yrek.nouncer.store.PointStore;

class DummyPointStore implements PointStore {
    final ArrayList<Point> points = new ArrayList<Point>();

    @Override
    public List<Point> getPoints(long minTimestamp, long maxTimestamp, int maxPoints) {
        ArrayList<Point> list = new ArrayList<Point>();
        int i = 0;
        synchronized (points) {
            while (i < points.size() && points.get(i).getTime() < minTimestamp) {
                i++;
            }
            while (i < points.size() && (maxPoints <= 0 || list.size() < maxPoints)) {
                Point point = points.get(i);
                if (point.getTime() > maxTimestamp) {
                    break;
                }
                list.add(point);
                i++;
            }
        }
        return list;
    }

    @Override
    public boolean addPoint(Point point, String tag) {
        synchronized (points) {
            if (!points.isEmpty()) {
                if (points.get(points.size() - 1).getTime() >= point.getTime()) {
                    return false;
                }
            }
            points.add(point);
        }
        return true;
    }
}

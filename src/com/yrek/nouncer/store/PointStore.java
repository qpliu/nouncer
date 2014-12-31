package com.yrek.nouncer.store;

import java.util.List;

import com.yrek.nouncer.data.Point;

public interface PointStore {
    public List<Point> getPoints(long minTimestamp, long maxTimestamp, int maxPoints);
    public boolean addPoint(Point point, String tag);
}

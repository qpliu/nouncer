package com.yrek.nouncer.processor;

import com.yrek.nouncer.data.Point;

// https://developers.google.com/maps/documentation/utilities/polylinealgorithm
public class PolylineDecoder {
    private final PointReceiver receiver;
    private final long t0;
    private final long dt;

    public PolylineDecoder(PointReceiver receiver) {
        this(receiver, 0, 1000);
    }

    public PolylineDecoder(PointReceiver receiver, long t0, long dt) {
        this.receiver = receiver;
        this.t0 = t0;
        this.dt = dt;
    }

    public void process(String polyline) {
        long t = t0;
        long[] p = new long[] { 0, 0 };
        long[] dp = new long[] { 0, 0 };
        int index = 0;
        int shift = 0;
        for (int i = 0; i < polyline.length(); i++) {
            int chunk = (((int) polyline.charAt(i))&127) - 63;
            dp[index] |= (chunk & 31) << shift;
            shift += 5;
            if ((chunk & 32) != 0) {
                continue;
            }
            shift = 0;
            if ((dp[index] & 1) == 1) {
                dp[index] = ~dp[index];
            }
            dp[index] >>= 1;
            if (index == 0) {
                index = 1;
                continue;
            }
            index = 0;
            p[0] += dp[0];
            p[1] += dp[1];
            final double lat = p[0] * 1e-5;
            final double lng = p[1] * 1e-5;
            final long time = t;
            receiver.receivePoint(new Point() {
                @Override public double getLatitude() { return lat; }
                @Override public double getLongitude() { return lng; }
                @Override public double getElevation() { return 0; }
                @Override public long getTime() { return time; }
            });
            dp[0] = 0;
            dp[1] = 0;
            t += dt;
        }
    }
}

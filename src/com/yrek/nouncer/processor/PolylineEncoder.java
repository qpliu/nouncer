package com.yrek.nouncer.processor;

import com.yrek.nouncer.data.Point;

// https://developers.google.com/maps/documentation/utilities/polylinealgorithm
public class PolylineEncoder implements PointReceiver {
    private final StringBuilder buffer = new StringBuilder();
    private long lat = 0;
    private long lng = 0;

    @Override
    public void receivePoint(Point point) {
        long lt = Math.round(point.getLatitude()*1e5);
        long lg = Math.round(point.getLongitude()*1e5);
        encode(lt - lat);
        encode(lg - lng);
        lat = lt;
        lng = lg;
    }

    private void encode(long val) {
        val <<= 1;
        if (val < 0) {
            val = ~val;
        }
        for (;;) {
            long ch = val & 31;
            val >>>= 5;
            if (val != 0) {
                ch |= 32;
            }
            buffer.append((char) (ch + 63));
            if (val == 0) {
                break;
            }
        }
    }

    public void reset() {
        buffer.setLength(0);
        lat = 0;
        lng = 0;
    }

    public String getPolyline() {
        return buffer.toString();
    }
}

package com.yrek.nouncer;

import java.io.InputStream;
import java.io.IOException;
import java.net.URL;

import android.util.JsonReader;

import com.yrek.nouncer.rest.JsonRestClient;

public class UsgsNed {
    public static Runnable queryElevation(final double lat, final double lng, final Receiver receiver) {
        return new Runnable() {
            @Override public void run() {
                try {
                    JsonRestClient.request(new URL("http://ned.usgs.gov/epqs/pqs.php"), null, new JsonRestClient.Parameters().add("x", String.valueOf(lng)).add("y", String.valueOf(lat)).add("units", "Meters").add("output", "json"), new JsonRestClient.ResponseReader() {
                        @Override public void onError(int responseCode, InputStream err) throws IOException {
                            receiver.onError();
                        }
                        @Override public void onResponse(int responseCode, JsonReader reader) throws IOException {
                            double elevation = -1000000;
                            reader.beginObject();
                            while (reader.hasNext()) {
                                String name = reader.nextName();
                                if ("USGS_Elevation_Point_Query_Service".equals(name)) {
                                    reader.beginObject();
                                    while (reader.hasNext()) {
                                        name = reader.nextName();
                                        if ("Elevation_Query".equals(name)) {
                                            reader.beginObject();
                                            while (reader.hasNext()) {
                                                name = reader.nextName();
                                                if ("Elevation".equals(name)) {
                                                    elevation = reader.nextDouble();
                                                } else {
                                                    reader.skipValue();
                                                }
                                            }
                                        } else {
                                            reader.skipValue();
                                        }
                                    }
                                    reader.endObject();
                                } else {
                                    reader.skipValue();
                                }
                            }
                            reader.endObject();
                            receiver.onResult(elevation);
                        }
                    });
                } catch (IOException e) {
                    receiver.onError();
                }
            }
        };
    }

    public interface Receiver {
        public void onError();
        public void onResult(double elevation);
    }
}

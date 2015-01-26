package com.yrek.nouncer;

import java.io.InputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.util.JsonReader;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.yrek.nouncer.data.Location;
import com.yrek.nouncer.data.Route;
import com.yrek.nouncer.external.ExternalSource;
import com.yrek.nouncer.external.Link;
import com.yrek.nouncer.processor.PointProcessor;
import com.yrek.nouncer.processor.PolylineDecoder;
import com.yrek.nouncer.rest.JsonRestClient;

class StravaWidget extends Widget {
    private static final String STRAVA = "strava";
    private static final String ACCESS_TOKEN = "access_token";
    private final String clientId;
    private final String clientSecret;
    private final ArrayList<Segment> segments = new ArrayList<Segment>();
    private final ArrayAdapter<Segment> segmentAdapter;
    private ExternalSource stravaSource;
    private String accessToken = null;
    private boolean fetching = false;

    StravaWidget(final Main activity, int id) {
        super(activity, id);
        clientId = getResourceString(activity, "strava_client_id");
        clientSecret = getResourceString(activity, "strava_client_secret");
        segmentAdapter = new ArrayAdapter<Segment>(activity, R.layout.strava_segment_list_entry) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = activity.getLayoutInflater().inflate(R.layout.strava_segment_list_entry, parent, false);
                }
                getItem(position).render(convertView);
                return convertView;
            }
        };
        ((AdapterView) view.findViewById(R.id.segment_list)).setAdapter(segmentAdapter);
        view.findViewById(R.id.refresh_button).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                synchronized (segments) {
                    segments.clear();
                }
                refreshSegments();
            }
        });
    }

    private static String getResourceString(Context context, String name) {
        try {
            String s = context.getString(R.string.class.getDeclaredField(name).getInt(null));
            return s.trim().length() > 0 ? s : null;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (NoSuchFieldException e) {
        }
        return null;
    }

    boolean available() {
        return (clientId != null && clientSecret != null) || accessToken != null;
    }

    @Override
    public void onServiceConnected() {
        if (stravaSource == null) {
            stravaSource = activity.store.getLocationLinkStore().getExternalSource(STRAVA);
            if (stravaSource == null) {
                stravaSource = activity.store.getLocationLinkStore().addExternalSource(STRAVA);
            }
        }
    }

    void enter() {
        boolean segmentsEmpty;
        synchronized (segments) {
            segmentsEmpty = segments.isEmpty();
        }
        if (segmentsEmpty) {
            refreshSegments();
        } else {
            showSegmentList.run();
        }
        activity.show(activity.tabsWidget, this);
    }

    private void refreshSegments() {
        synchronized (segments) {
            if (fetching) {
                return;
            }
            fetching = true;
        }
        NetworkInfo networkInfo = ((ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected()) {
            activity.notificationWidget.show("Strava: No network connection");
            return;
        }
        if (accessToken == null) {
            accessToken = stravaSource.getAttributes().get(ACCESS_TOKEN);
            if (accessToken == null) {
                activity.startActivity(new Intent(Intent.ACTION_VIEW, new Uri.Builder().scheme("https").authority("www.strava.com").path("/oauth/authorize").appendQueryParameter("client_id", clientId).appendQueryParameter("response_type", "code").appendQueryParameter("redirect_uri", new Uri.Builder().scheme("nouncer").authority("localhost").path("/strava").toString()).build()));
                return;
            }
        }
        activity.threadPool.execute(fetchSegments(0));
        showProgressBar.run();
    }

    private final Runnable showProgressBar = new Runnable() {
        @Override
        public void run() {
            view.findViewById(R.id.progress_bar).setVisibility(View.VISIBLE);
            view.findViewById(R.id.refresh_button).setVisibility(View.GONE);
            view.findViewById(R.id.segment_list).setVisibility(View.GONE);
        }
    };

    private final Runnable showSegmentList = new Runnable() {
        @Override
        public void run() {
            view.findViewById(R.id.progress_bar).setVisibility(View.GONE);
            view.findViewById(R.id.refresh_button).setVisibility(View.VISIBLE);
            view.findViewById(R.id.segment_list).setVisibility(View.VISIBLE);
            segmentAdapter.clear();
            synchronized (segments) {
                segmentAdapter.addAll(segments);
            }
        }
    };

    private Runnable fetchSegments(final int page) {
        return new Runnable() {
            @Override public void run() {
                try {
                    JsonRestClient.request(new URL("https://www.strava.com/api/v3/segments/starred" + (page > 0 ? "?page=" + page : "")), new JsonRestClient.Parameters().add("Authorization", "Bearer " + accessToken), null, new JsonRestClient.ResponseReader() {
                        @Override public void onError(int responseCode, InputStream err) throws IOException {
                            post(showSegmentList);
                            if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                                stravaSource.setAttribute(ACCESS_TOKEN, null);
                                showMessage("Strava: Deauthorized");
                            } else {
                                showMessage("Strava segments: HTTP error:" + responseCode);
                            }
                        }
                        @Override public void onResponse(int responseCode, JsonReader reader) throws IOException {
                            reader.beginArray();
                            while (reader.hasNext()) {
                                reader.beginObject();
                                String id = null;
                                String name = null;
                                double startLat = 0.0;
                                double endLat = 0.0;
                                double endLng = 0.0;
                                double startLng = 0.0;
                                double dist = 0.0;
                                double avgGrade = 0.0;
                                while (reader.hasNext()) {
                                    String key = reader.nextName();
                                    if ("id".equals(key)) {
                                        id = String.valueOf(reader.nextLong());
                                    } else if ("name".equals(key)) {
                                        name = reader.nextString();
                                    } else if ("start_latitude".equals(key)) {
                                        startLat = reader.nextDouble();
                                    } else if ("start_longitude".equals(key)) {
                                        startLng = reader.nextDouble();
                                    } else if ("end_latitude".equals(key)) {
                                        endLat = reader.nextDouble();
                                    } else if ("end_longitude".equals(key)) {
                                        endLng = reader.nextDouble();
                                    } else if ("distance".equals(key)) {
                                        dist = reader.nextDouble();
                                    } else if ("average_grade".equals(key)) {
                                        avgGrade = reader.nextDouble();
                                    } else {
                                        reader.skipValue();
                                    }
                                }
                                reader.endObject();
                                synchronized (segments) {
                                    segments.add(new Segment(id, name, startLat, startLng, endLat, endLng, dist, avgGrade));
                                }
                            }
                            reader.endArray();
                            post(showSegmentList);
                        }
                    });
                } catch (final IOException e) {
                    showMessage("Strava segments: IO error:" + e.getMessage());
                } finally {
                    synchronized (segments) {
                        fetching = false;
                    }
                }
            }
        };
    }

    private class Segment {
        final String id;
        final String name;
        final double startLat;
        final double startLng;
        final double endLat;
        final double endLng;
        final double dist;
        final double avgGrade;
        Link<Location> startLink;
        Link<Location> endLink;
        Link<Route> routeLink;

        Segment(String id, String name, double startLat, double startLng, double endLat, double endLng, double dist, double avgGrade) {
            this.id = id;
            this.name = name;
            this.startLat = startLat;
            this.startLng = startLng;
            this.endLat = endLat;
            this.endLng = endLng;
            this.dist = dist;
            this.avgGrade = avgGrade;
            this.startLink = null;
            this.endLink = null;
            this.routeLink = null;
        }

        void render(View v) {
            if (startLink == null) {
                startLink = activity.store.getLocationLinkStore().getLink(stravaSource, "s" + id);
            }
            if (endLink == null) {
                endLink = activity.store.getLocationLinkStore().getLink(stravaSource, "e" + id);
            }
            if (routeLink == null) {
                routeLink = activity.store.getRouteLinkStore().getLink(stravaSource, id);
            }
            ((TextView) v.findViewById(R.id.name)).setText(String.format("%s (%.1fmi %.1f%%)", name, dist*0.000621371, avgGrade));
            if (startLink != null) {
                v.findViewById(R.id.start_name_text).setVisibility(View.VISIBLE);
                ((TextView) v.findViewById(R.id.start_name_text)).setText(startLink.getItem().getName());
                v.findViewById(R.id.start_name_text).setOnClickListener(showLocationOnClick(startLink.getItem()));
                v.findViewById(R.id.add_start_button).setVisibility(View.GONE);
            } else {
                v.findViewById(R.id.start_name_text).setVisibility(View.GONE);
                v.findViewById(R.id.add_start_button).setVisibility(View.VISIBLE);
                v.findViewById(R.id.add_start_button).setOnClickListener(addLocationOnClick("Start of "+name, "s"+id, startLat, startLng, true));
            }
            if (endLink != null) {
                v.findViewById(R.id.end_name_text).setVisibility(View.VISIBLE);
                ((TextView) v.findViewById(R.id.end_name_text)).setText(endLink.getItem().getName());
                v.findViewById(R.id.end_name_text).setOnClickListener(showLocationOnClick(endLink.getItem()));
                v.findViewById(R.id.add_end_button).setVisibility(View.GONE);
            } else {
                v.findViewById(R.id.end_name_text).setVisibility(View.GONE);
                v.findViewById(R.id.add_end_button).setVisibility(View.VISIBLE);
                v.findViewById(R.id.add_end_button).setOnClickListener(addLocationOnClick("End of "+name, "e"+id, endLat, endLng, false));
            }
            if (routeLink != null) {
                v.findViewById(R.id.route_name_text).setVisibility(View.VISIBLE);
                ((TextView) v.findViewById(R.id.route_name_text)).setText(routeLink.getItem().getName());
                v.findViewById(R.id.route_name_text).setOnClickListener(showRouteOnClick);
                v.findViewById(R.id.add_route_button).setVisibility(View.GONE);
            } else {
                v.findViewById(R.id.route_name_text).setVisibility(View.GONE);
                v.findViewById(R.id.add_route_button).setVisibility(View.VISIBLE);
                v.findViewById(R.id.add_route_button).setOnClickListener(addRouteOnClick);
            }
        }

        private View.OnClickListener showLocationOnClick(final Location location) {
            return new View.OnClickListener() {
                @Override public void onClick(View v) {
                    activity.locationWidget.show(location);
                }
            };
        }

        private View.OnClickListener addLocationOnClick(final String addName, final String addId, final double lat, final double lng, final boolean isStart) {
            final double[] elev = new double[] { -1000000 };
            final Runnable showAddLocation = new Runnable() {
                @Override public void run() {
                    activity.addLocationWidget.show(new AddLocationWidget.OnFinish() {
                        @Override public void onFinish(final Location location) {
                            if (location != null) {
                                Link<Location> link = activity.store.getLocationLinkStore().addLink(location, stravaSource, addId);
                                if (isStart) {
                                    startLink = link;
                                } else {
                                    endLink = link;
                                }
                            }
                            enter();
                        }
                    }, addName, lat, lng, elev[0]);
                }
            };
            final Runnable getElevation = UsgsNed.queryElevation(startLat, startLng, new UsgsNed.Receiver() {
                @Override public void onError() {
                    post(showAddLocation);
                }
                @Override public void onResult(double el) {
                    elev[0] = el;
                    post(showAddLocation);
                }
            });
            return new View.OnClickListener() {
                @Override public void onClick(View v) {
                    showProgressBar.run();
                    activity.threadPool.execute(getElevation);
                }
            };
        }

        private final View.OnClickListener showRouteOnClick = new View.OnClickListener() {
            @Override public void onClick(View v) {
                activity.routeWidget.show(routeLink.getItem());
            }
        };

        private final View.OnClickListener addRouteOnClick = new View.OnClickListener() {
            @Override public void onClick(View v) {
                showProgressBar.run();
                activity.threadPool.execute(prepareAddRoute);
            }
        };

        private final Runnable prepareAddRoute = new Runnable() {
            @Override public void run() {
                try {
                    JsonRestClient.request(new URL("https://www.strava.com/api/v3/segments/" + id), new JsonRestClient.Parameters().add("Authorization", "Bearer " + accessToken), null, new JsonRestClient.ResponseReader() {
                        @Override public void onError(int responseCode, InputStream err) throws IOException {
                            post(showSegmentList);
                            if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                                stravaSource.setAttribute(ACCESS_TOKEN, null);
                                showMessage("Strava: Deauthorized");
                            } else {
                                showMessage("Strava segment: HTTP error:" + responseCode);
                            }
                        }
                        @Override public void onResponse(int responseCode, JsonReader reader) throws IOException {
                            String polyline = null;
                            reader.beginObject();
                            while (reader.hasNext()) {
                                String key = reader.nextName();
                                if ("map".equals(key)) {
                                    reader.beginObject();
                                    while (reader.hasNext()) {
                                        key = reader.nextName();
                                        if ("polyline".equals(key)) {
                                            polyline = reader.nextString();
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
                            post(addRoute(polyline));
                        }
                    });
                } catch (final IOException e) {
                    post(showSegmentList);
                    showMessage("Strava segment: IO error:" + e.getMessage());
                }
            }
        };

        private ArrayList<Location> getPolylineLocations(String polyline) {
            final ArrayList<Location> locations = new ArrayList<Location>();
            new PolylineDecoder(new PointProcessor(activity.store.getLocationStore(), new PointProcessor.Listener() {
                @Override public void receiveEntry(Location location, long entryTime, double entryHeading, double entrySpeed, long timestamp) {
                    locations.add(location);
                }
                @Override public void receiveExit(Location location, long exitTime, double exitHeading, double exitSpeed, long timestamp) {
                }
            }, 75.0, 80.0)).process(polyline);
            return locations;
        }

        private Runnable addRoute(final String polyline) {
            final ArrayList<Location> locations = getPolylineLocations(polyline);
            return new Runnable() {
                @Override public void run() {
                    activity.addRouteWidget.show(onAddRouteFinish(polyline), name, locations);
                }
            };
        }

        private AddRouteWidget.OnFinish onAddRouteFinish(final String polyline) {
            return new AddRouteWidget.OnFinish() {
                @Override public void onFinish(Route route) {
                    if (route != null) {
                        routeLink = activity.store.getRouteLinkStore().addLink(route, stravaSource, id);
                        routeLink.setAttribute("polyline", polyline);
                    }
                    enter();
                }
            };
        }

        void invalidateCache() {
            startLink = null;
            endLink = null;
            routeLink = null;
        }
    }

    public void onNewIntent(Intent intent) {
        if (!"/strava".equals(intent.getData().getPath())) {
            return;
        }
        List<String> codeParameter = intent.getData().getQueryParameters("code");
        if (codeParameter == null || codeParameter.size() != 1) {
            return;
        }
        final String code = codeParameter.get(0);
        activity.threadPool.execute(new Runnable() {
            @Override public void run() {
                try {
                    JsonRestClient.request(new URL("https://www.strava.com/oauth/token"), null, new JsonRestClient.Parameters().add("client_id", clientId).add("client_secret", clientSecret).add("code", code), new JsonRestClient.ResponseReader() {
                        @Override public void onError(int responseCode, InputStream err) throws IOException {
                            showMessage("Strava authorization: HTTP error:" + responseCode);
                        }
                        @Override public void onResponse(int responseCode, JsonReader reader) throws IOException {
                            String accessToken = null;
                            reader.beginObject();
                            while (reader.hasNext()) {
                                String name = reader.nextName();
                                if ("access_token".equals(name)) {
                                    accessToken = reader.nextString();
                                } else {
                                    reader.skipValue();
                                }
                            }
                            reader.endObject();
                            stravaSource.setAttribute(ACCESS_TOKEN, accessToken);
                            StravaWidget.this.accessToken = accessToken;
                            post(new Runnable() {
                                @Override public void run() {
                                    enter();
                                }
                            });
                        }
                    });
                } catch (IOException e) {
                    showMessage("Strava authorization: IO error:" + e.getMessage());
                }
            }
        });
    }

    public void invalidateCache() {
        synchronized (segments) {
            for (Segment s : segments) {
                s.invalidateCache();
            }
        }
    }
}

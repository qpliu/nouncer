package com.yrek.nouncer;

import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.util.JsonReader;
import android.view.View;

import com.yrek.nouncer.external.ExternalSource;
import com.yrek.nouncer.external.Link;
import com.yrek.nouncer.rest.JsonRestClient;

class StravaWidget extends Widget {
    private static final String STRAVA = "strava";
    private static final String ACCESS_TOKEN = "access_token";
    private final String clientId;
    private final String clientSecret;
    private String accessToken = null;

    StravaWidget(final Main activity, int id) {
        super(activity, id);
        clientId = activity.getString(R.string.strava_client_id);
        clientSecret = activity.getString(R.string.strava_client_secret);
    }

    void enter() {
        NetworkInfo networkInfo = ((ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected()) {
            activity.notificationWidget.show("Strava: No network connection");
            return;
        }
        if (accessToken == null) {
            ExternalSource src = activity.store.getLocationLinkStore().getExternalSource(STRAVA);
            if (src == null) {
                src = activity.store.getLocationLinkStore().addExternalSource(STRAVA);
            }
            accessToken = src.getAttributes().get(ACCESS_TOKEN);
            if (accessToken == null) {
                activity.startActivity(new Intent(Intent.ACTION_VIEW, new Uri.Builder().scheme("https").authority("www.strava.com").path("/oauth/authorize").appendQueryParameter("client_id", clientId).appendQueryParameter("response_type", "code").appendQueryParameter("redirect_uri", new Uri.Builder().scheme("nouncer").authority("localhost").path("/strava").toString()).build()));
                return;
            }
        }
        activity.notificationWidget.show("Strava: Not implemented: accessToken=" + accessToken);
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
                            showMessage("Strava: HTTP error:" + responseCode);
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
                            activity.store.getLocationLinkStore().getExternalSource(STRAVA).setAttribute(ACCESS_TOKEN, accessToken);
                            StravaWidget.this.accessToken = accessToken;
                            post(new Runnable() {
                                @Override public void run() {
                                    enter();
                                }
                            });
                        }
                    });
                } catch (final IOException e) {
                    showMessage("Strava: IO error:" + e.getMessage());
                }
            }
        });
    }
}

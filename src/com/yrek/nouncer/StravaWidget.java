package com.yrek.nouncer;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.view.View;

import com.yrek.nouncer.external.ExternalSource;
import com.yrek.nouncer.external.Link;

class StravaWidget extends Widget {
    private static final String STRAVA = "strava";
    private static final String ACCESS_TOKEN = "access_token";
    private String accessToken = null;

    StravaWidget(final Main activity, int id) {
        super(activity, id);
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
                activity.startActivity(new Intent(Intent.ACTION_VIEW, new Uri.Builder().scheme("https").authority("www.strava.com").path("/oauth/authorize").appendQueryParameter("client_id", activity.getString(R.string.strava_client_id)).appendQueryParameter("response_type", "code").appendQueryParameter("redirect_uri", new Uri.Builder().scheme("nouncer").authority("localhost").path("/strava").toString()).build()));
                return;
            }
        }
        activity.notificationWidget.show("Strava: Not implemented");
    }

    public void onNewIntent(Intent intent) {
        if (!"/strava".equals(intent.getData().getPath())) {
            return;
        }
        List<String> code = intent.getData().getQueryParameters("code");
        if (code == null || code.size() != 1) {
            return;
        }
        // get access token from https://www.strava.com/oauth/token
    }
}

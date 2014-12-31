package com.yrek.nouncer;

import android.content.Context;
import android.content.Intent;
import android.view.View;

class StartStopWidget extends Widget {
    private final View serviceRunning;
    private final View serviceNotRunning;
    private AnnouncerService announcerService = null;

    StartStopWidget(final Context context, View view) {
        super(view);
        this.serviceRunning = view.findViewById(R.id.service_running);
        this.serviceNotRunning = view.findViewById(R.id.service_not_running);
        showNothing.run();
        view.findViewById(R.id.start_button).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                context.startService(new Intent(context, AnnouncerService.class));
                showRunning.run();
            }
        });
        view.findViewById(R.id.stop_button).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                AnnouncerService announcerService = StartStopWidget.this.announcerService;
                if (announcerService != null) {
                    announcerService.stop();
                }
                showNothing.run();
            }
        });
    }

    private final Runnable showRunning = new Runnable() {
        @Override public void run() {
            serviceRunning.setVisibility(View.VISIBLE);
            serviceNotRunning.setVisibility(View.GONE);
        }
    };

    private final Runnable showNotRunning = new Runnable() {
        @Override public void run() {
            serviceRunning.setVisibility(View.GONE);
            serviceNotRunning.setVisibility(View.VISIBLE);
        }
    };

    private final Runnable showNothing = new Runnable() {
        @Override public void run() {
            serviceRunning.setVisibility(View.GONE);
            serviceNotRunning.setVisibility(View.GONE);
        }
    };

    public void onServiceConnected(AnnouncerService announcerService) {
        this.announcerService = announcerService;
        post(announcerService.isStarted() ? showRunning : showNotRunning);
    }

    public void onServiceDisconnected() {
        this.announcerService = null;
        post(showNothing);
    }
}

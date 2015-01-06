package com.yrek.nouncer;

import android.content.Intent;
import android.view.View;

class StartStopWidget extends Widget {
    private final View startButton;
    private final View stopButton;
    private final View serviceRunning;
    private final View serviceNotRunning;
    private AnnouncerService announcerService = null;

    StartStopWidget(final Main activity, int id) {
        super(activity, id);
        this.startButton = view.findViewById(R.id.start_button);
        this.stopButton = view.findViewById(R.id.stop_button);
        this.serviceRunning = view.findViewById(R.id.service_running);
        this.serviceNotRunning = view.findViewById(R.id.service_not_running);
        showNothing.run();
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                activity.startService(new Intent(activity, AnnouncerService.class));
                showRunning.run();
            }
        });
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                AnnouncerService announcerService = StartStopWidget.this.announcerService;
                if (announcerService != null) {
                    announcerService.stop();
                }
                showNotRunning.run();
            }
        });
    }

    private final Runnable showRunning = new Runnable() {
        @Override public void run() {
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            serviceRunning.setVisibility(View.VISIBLE);
            serviceNotRunning.setVisibility(View.GONE);
        }
    };

    private final Runnable showNotRunning = new Runnable() {
        @Override public void run() {
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            serviceRunning.setVisibility(View.GONE);
            serviceNotRunning.setVisibility(View.VISIBLE);
        }
    };

    private final Runnable showNothing = new Runnable() {
        @Override public void run() {
            startButton.setEnabled(false);
            stopButton.setEnabled(false);
            serviceRunning.setVisibility(View.GONE);
            serviceNotRunning.setVisibility(View.GONE);
        }
    };

    @Override
    public void onServiceConnected(AnnouncerService announcerService) {
        this.announcerService = announcerService;
        post(announcerService.isStarted() ? showRunning : showNotRunning);
    }

    @Override
    public void onServiceDisconnected() {
        this.announcerService = null;
        post(showNothing);
    }
}

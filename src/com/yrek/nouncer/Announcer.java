package com.yrek.nouncer;

import android.content.Context;
import android.os.PowerManager;
import android.speech.tts.TextToSpeech;

import com.yrek.nouncer.data.Route;
import com.yrek.nouncer.processor.RouteProcessor;

class Announcer implements RouteProcessor.Listener {
    private final Context context;
    private final PowerManager.WakeLock wakeLock;
    private TextToSpeech textToSpeech = null;
    private boolean initialized = false;

    Announcer(Context context) {
        this.context = context;
        this.wakeLock = ((PowerManager) context.getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, getClass().getName());
    }

    public void start() {
        synchronized (onInitListener) {
            if (textToSpeech != null) {
                return;
            }
            initialized = false;
            textToSpeech = new TextToSpeech(context, onInitListener);
        }
    }

    public void stop() {
        TextToSpeech tts;
        synchronized (onInitListener) {
            tts = textToSpeech;
            textToSpeech = null;
        }
        if (tts != null) {
            tts.shutdown();
        }
    }

    private final TextToSpeech.OnInitListener onInitListener = new TextToSpeech.OnInitListener() {
        @Override
        public void onInit(int status) {
            if (status == TextToSpeech.SUCCESS) {
                synchronized(onInitListener) {
                    initialized = true;
                }
            } else {
                TextToSpeech tts;
                synchronized(onInitListener) {
                    tts = textToSpeech;
                    initialized = false;
                    textToSpeech = null;
                }
                tts.shutdown();
            }
        }
    };

    @Override
    public void receiveEntry(Route route, long startTime, int routeIndex, long entryTime) {
        announce(route.getRoutePoint(routeIndex).getEntryAnnouncement(), startTime, entryTime, route.getName(), route.getRoutePoint(routeIndex).getLocation().getName());
    }

    @Override
    public void receiveExit(Route route, long startTime, int routeIndex, long exitTime) {
        announce(route.getRoutePoint(routeIndex).getExitAnnouncement(), startTime, exitTime, route.getName(), route.getRoutePoint(routeIndex).getLocation().getName());
    }

    private void announce(String announcement, long startTime, long timestamp, String routeName, String locationName) {
        if (announcement == null) {
            return;
        }
        TextToSpeech tts = null;
        synchronized (onInitListener) {
            if (initialized) {
                tts = textToSpeech;
            }
        }
        if (tts == null) {
            return;
        }
        wakeLock.acquire();
        textToSpeech.speak(String.format(announcement, timestamp, (timestamp - startTime) / 60000L, ((timestamp - startTime) % 60000L) / 1000L, routeName, locationName), TextToSpeech.QUEUE_ADD, null);
        wakeLock.release();
    }
}

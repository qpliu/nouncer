package com.yrek.nouncer.processor;

import com.yrek.nouncer.store.AvailabilityStore;

public class AvailabilityProcessor {
    private final AvailabilityStore availabilityStore;
    private long lastAvailableTime = 0L;
    private long lastUnavailableTime = 0L;

    public AvailabilityProcessor(AvailabilityStore availabilityStore) {
        this.availabilityStore = availabilityStore;
    }

    public synchronized void available(long timestamp) {
        if (timestamp < Math.max(lastAvailableTime, lastUnavailableTime)) {
            return;
        }
        if (lastUnavailableTime >= lastAvailableTime) {
            availabilityStore.addUnavailableTime(lastUnavailableTime, timestamp);
        }
        lastAvailableTime = timestamp;
    }

    public synchronized void unavailable(long timestamp) {
        if (timestamp < Math.max(lastAvailableTime, lastUnavailableTime)) {
            return;
        }
        if (lastUnavailableTime < lastAvailableTime) {
            lastUnavailableTime = timestamp;
        }
    }
}

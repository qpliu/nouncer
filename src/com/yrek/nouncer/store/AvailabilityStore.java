package com.yrek.nouncer.store;

public interface AvailabilityStore {
    public boolean wasUnavailable(long minTimestamp, long maxTimestamp);
    public void addUnavailableTime(long startTime, long endTime);
    public void deleteOlderThan(long timestamp);
}

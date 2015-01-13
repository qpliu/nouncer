package com.yrek.nouncer.data;

public interface RoutePoint {
    public Location getLocation();
    public String getEntryAnnouncement();
    public String getExitAnnouncement();
    public void setEntryAnnouncement(String entryAnnouncement);
    public void setExitAnnouncement(String exitAnnouncement);
}

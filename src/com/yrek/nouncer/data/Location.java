package com.yrek.nouncer.data;

public interface Location {
    public String getName();
    public double getLatitude();
    public double getLongitude();
    public double getElevation();
    public boolean isHidden();
    public void delete();
}

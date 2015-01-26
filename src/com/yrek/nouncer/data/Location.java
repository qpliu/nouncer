package com.yrek.nouncer.data;

public interface Location {
    public String getName();
    public void setName(String name);
    public double getLatitude();
    public double getLongitude();
    public double getElevation();
    public boolean isHidden();
}

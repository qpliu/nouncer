package com.yrek.nouncer.external;

import java.util.Map;

public interface Link<T> {
    public ExternalSource getExternalSource();
    public String getExternalId();
    public Map<String,String> getExternalAttributes();
    public void setExternalAttribute(String key, String value);
    public T getItem();
}

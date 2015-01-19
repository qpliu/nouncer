package com.yrek.nouncer.external;

import java.util.Map;

public interface Link<T> {
    public ExternalSource getExternalSource();
    public String getExternalId();
    public Map<String,String> getAttributes();
    public void setAttribute(String key, String value);
    public T getItem();
}

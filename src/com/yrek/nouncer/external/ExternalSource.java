package com.yrek.nouncer.external;

import java.util.Map;

public interface ExternalSource {
    public String getExternalSourceId();
    public Map<String,String> getAttributes();
    public void setAttribute(String key, String value);
}

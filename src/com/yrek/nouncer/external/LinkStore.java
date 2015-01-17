package com.yrek.nouncer.external;

import java.util.Collection;

public interface LinkStore<T> {
    public Link<T> getLink(ExternalSource externalSource, String externalId);
    public Link<T> getLink(T item, ExternalSource externalSource);
    public Collection<Link<T>> getLinks(T item);
    public void delete(Link<T> link);
    public Link<T> addLink(T item, ExternalSource externalSource, String externalId);
    public ExternalSource getExternalSource(String externalSourceId);
    public Collection<ExternalSource> getExternalSources();
    public void delete(ExternalSource externalSource);
    public ExternalSource addExternalSource(String externalSourceId);
}

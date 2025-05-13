package GroupBased.Model;

import core.DTNHost;

import java.util.HashSet;
import java.util.Set;

public class RawGroup {
    private DTNHost subscriber;
    private Set<FilterData> filters;

    public RawGroup() {
        this.filters = new HashSet<>();
    }

    public DTNHost getSubscriber() {
        return subscriber;
    }

    public void setSubscriber(DTNHost subscriber) {
        this.subscriber = subscriber;
    }

    public Set<FilterData> getEvents() {
        return filters;
    }

    public void setEvents(Set<FilterData> events) {
        this.filters = events;
    }
}

package routing.util;

import core.DTNHost;

import java.util.Map;

public interface SubscriberKey {
    Map<DTNHost, Integer> getKeys();
}

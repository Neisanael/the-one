package report;

import core.DTNHost;
import core.SimClock;
import core.UpdateListener;
import routing.GrouperRouter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NumberKeyPerSubscriberReport extends Report implements UpdateListener {
    private final List<int[]> subscriberKeyTimeline = new ArrayList<>();
    private double lastUpdate = 0;
    private final double threshold = 900; //30 minutes

    @Override
    public void updated(List<DTNHost> hosts) {
        if (isWarmup()) {
            return;
        }

        double currentTime = SimClock.getTime();
        if ((currentTime - lastUpdate) < threshold) {
            return;
        }
        lastUpdate = currentTime;

        Map<DTNHost, Integer> maxKeyPerSubscriber = new HashMap<>();

        for (DTNHost host : hosts) {
            if (host.getRouter() instanceof GrouperRouter) {
                GrouperRouter router = (GrouperRouter) host.getRouter();
                /*Map<DTNHost, Integer> localKeys = router.getKeys();

                if (localKeys != null && !localKeys.isEmpty()) {
                    for (Map.Entry<DTNHost, Integer> entry : localKeys.entrySet()) {
                        DTNHost subscriber = entry.getKey();
                        int count = entry.getValue();
                        maxKeyPerSubscriber.merge(subscriber, count, Math::max);
                    }
                }*/

            }
        }

        int totalSubscribers = maxKeyPerSubscriber.size();
        int totalKeys = maxKeyPerSubscriber.values().stream().mapToInt(Integer::intValue).sum();

        subscriberKeyTimeline.add(new int[]{totalSubscribers, totalKeys});
    }
}

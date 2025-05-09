package core.GroupBased;

import core.DTNHost;

import java.util.Set;
import java.util.stream.Collectors;

public class MergedInterval {
    private final int start;
    private final int end;
    private final Set<DTNHost> senders;

    public MergedInterval(int start, int end, Set<DTNHost> senders) {
        this.start = start;
        this.end = end;
        this.senders = senders;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public Set<DTNHost> getSenders() {
        return senders;
    }

    @Override
    public String toString() {
        String senderNames = senders.stream()
                .map(DTNHost::toString)
                .sorted()
                .collect(Collectors.joining(","));
        return String.format("G(%d,%d)<%s>", start, end, senderNames);
    }
}

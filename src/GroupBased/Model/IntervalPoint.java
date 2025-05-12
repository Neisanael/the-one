package GroupBased.Model;

import core.DTNHost;

public class IntervalPoint implements Comparable<IntervalPoint> {
    private int value;
    private boolean isStart;
    private DTNHost sender;

    public IntervalPoint(int value, boolean isStart, DTNHost sender) {
        this.value = value;
        this.isStart = isStart;
        this.sender = sender;
    }

    @Override
    public int compareTo(IntervalPoint other) {
        if (this.value != other.value) {
            return Integer.compare(this.value, other.value);
        }
        // Start points come before end points when values are equal
        return Boolean.compare(other.isStart, this.isStart);
    }

    public int getValue() {
        return value;
    }

    public boolean isStart() {
        return isStart;
    }

    public DTNHost getSender() {
        return sender;
    }
}
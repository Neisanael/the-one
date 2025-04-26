package GroupBased.Model;

import core.GroupBased.Subscriber;

import java.util.List;

public class NumericGroupData {
    private final int start;
    private final int end;
    private final List<Subscriber> subscribers;

    public NumericGroupData(int start, int end, List<Subscriber> subscribers) {
        this.start = start;
        this.end = end;
        this.subscribers = subscribers;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public List<Subscriber> getSubscribers() {
        return subscribers;
    }
}

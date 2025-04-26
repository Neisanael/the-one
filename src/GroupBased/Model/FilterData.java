package GroupBased.Model;

public class FilterData {
    private final boolean issue;
    private final int start;
    private final int end;

    public FilterData(boolean issue, int start, int end) {
        this.issue = issue;
        this.start = start;
        this.end = end;
    }

    public boolean getIssue() {
        return issue;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public String toString() {
        return issue + ":" + start + ":" + end;
    }
}

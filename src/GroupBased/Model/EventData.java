package GroupBased.Model;

public class EventData {
    private final boolean issue;
    private final int num;

    public EventData(boolean issue, int num) {
        this.issue = issue;
        this.num = num;
    }

    public boolean getIssue() {
        return issue;
    }

    public int getNum() {
        return num;
    }

    public String toString() {
        return issue + ":" + num;
    }
}

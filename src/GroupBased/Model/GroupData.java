package GroupBased.Model;

import java.util.List;

public class GroupData {
    private final boolean domain;
    private final List<NumericGroupData> groups;

    public GroupData(boolean domain, List<NumericGroupData> groups) {
        this.domain = domain;
        this.groups = groups;
    }

    public boolean getDomain() {
        return domain;
    }

    public List<NumericGroupData> getGroups() {
        return groups;
    }
}

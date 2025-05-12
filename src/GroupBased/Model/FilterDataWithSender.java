package GroupBased.Model;

import core.DTNHost;

public class FilterDataWithSender {
    private FilterData filterData;
    private DTNHost sender;

    public FilterDataWithSender(FilterData filterData, DTNHost sender) {
        this.filterData = filterData;
        this.sender = sender;
    }

    public FilterData getFilterData() {
        return filterData;
    }

    public DTNHost getSender() {
        return sender;
    }
}
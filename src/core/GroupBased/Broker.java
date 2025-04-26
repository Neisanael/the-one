package core.GroupBased;

import GroupBased.Model.EventData;
import GroupBased.Model.FilterData;
import GroupBased.PropertySettings;
import core.*;
import movement.MovementModel;
import routing.MessageRouter;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static GroupBased.LoremIpsumGenerator.generateLoremIpsum;

public class Broker extends DTNHost implements PropertySettings {
    private List<MergedInterval> groups;
    private Map<DTNHost, BigInteger> publicSecretKey;
    private List<Set<byte[]>> encryptedEentsGrouped;

    /**
     * Creates a new DTNHost.
     *
     * @param msgLs        Message listeners
     * @param movLs        Movement listeners
     * @param groupId      GroupID of this host
     * @param interf       List of NetworkInterfaces for the class
     * @param comBus       Module communication bus object
     * @param mmProto      Prototype of the movement model of this host
     * @param mRouterProto Prototype of the message router of this host
     */
    public Broker(List<MessageListener> msgLs, List<MovementListener> movLs, String groupId, List<NetworkInterface> interf, ModuleCommunicationBus comBus, MovementModel mmProto, MessageRouter mRouterProto) {
        super(msgLs, movLs, groupId, interf, comBus, mmProto, mRouterProto);
        publicSecretKey = new HashMap<>();
        encryptedEentsGrouped = new ArrayList<>();
    }

    public Map<DTNHost, BigInteger> getPublicSecretKey() {
        return publicSecretKey;
    }

    public void setPublicSecretKey(Map<DTNHost, BigInteger> publicSecretKey) {
        this.publicSecretKey = publicSecretKey;
    }

    public void addPublicSecretKey(DTNHost host, BigInteger publicSecretKey) {
        this.publicSecretKey.put(host, publicSecretKey);
    }

    public void makeGroups(){
        groups = MakeGroup();
        GroupedEvents();
    }

    private void GroupedEvents(){
        for(Message msg : getMessageCollection()){
            if(msg.getProperty(EVENTS) != null && msg.getProperty(EVENTS) instanceof Set<?>){
                try {
                    Set<EventData> restoredSet = (Set<EventData>) msg.getProperty(EVENTS);
                    for(EventData ed : restoredSet){
                        Set<byte[]> eventGroups = new HashSet<>();
                        if(getGroups() != null){
                            for(MergedInterval group : getGroups()){
                                eventGroups.add(encryptEvent(generateLoremIpsum(5), generateAESKey(256)));
                                if(isInRange(ed.getNum(), group.getStart(), group.getEnd())){
                                    for(DTNHost subscriber : group.getSenders()){
                                        if(getPublicSecretKey().containsKey(subscriber)){
                                            // TODO : seharusnya nanti ketika dibuat ulang grouping data sebelumnya terhapus dan menambah ulang lagi
                                            eventGroups.add(encryptEvent(String.valueOf(getPublicSecretKey().get(subscriber)), generateAESKey(256)));
                                        }
                                    }
                                }
                            }
                        }
                        encryptedEentsGrouped.add(eventGroups);
                    }
                } catch (ClassCastException e) {
                    System.err.println("Failed to cast - wrong generic type");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public static SecretKey generateAESKey(int keySize) throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(keySize);
        return keyGen.generateKey();
    }

    private byte[] encryptEvent(String event, SecretKey key) throws Exception {
        return encryptAES(event.getBytes(StandardCharsets.UTF_8), key);
    }

    private static byte[] encryptAES(byte[] data, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding"); // For demo; use GCM or CBC in production
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(data);
    }

    public List<Set<byte[]>> getEncryptedEventsGrouped() {
        return encryptedEentsGrouped;
    }

    public List<MergedInterval> getGroups() {
        return groups;
    }

    public static boolean isInRange(int value, int start, int end) {
        return value >= start && value <= end;
    }

    private List<MergedInterval> MakeGroup() {
        // First collect all FilterData with their senders
        List<FilterDataWithSender> allFilters = new ArrayList<>();

        for (Message msg : getMessageCollection()) {
            if (msg.getProperty(FILTERS) != null && msg.getProperty(FILTERS) instanceof Set<?>) {
                try {
                    Set<FilterData> restoredSet = (Set<FilterData>) msg.getProperty(FILTERS);
                    for (FilterData fd : restoredSet) {
                        allFilters.add(new FilterDataWithSender(fd, msg.getFrom()));
                    }
                    //System.out.println("Message from " + msg.getFrom() + " has filters: " + restoredSet);
                } catch (ClassCastException e) {
                    System.err.println("Failed to cast - wrong generic type");
                }
            }
        }

        if (allFilters.isEmpty()) {
            //System.out.println("No filters found in any messages");
            return Collections.emptyList();
        }

        // Group by issue first (true/false)
        Map<Boolean, List<FilterDataWithSender>> groupedByIssue = allFilters.stream()
                .collect(Collectors.groupingBy(fws -> fws.filterData.getIssue()));

        // Combined result list
        List<MergedInterval> combinedResults = new ArrayList<>();

        // Process true and false groups separately but combine results
        for (Map.Entry<Boolean, List<FilterDataWithSender>> entry : groupedByIssue.entrySet()) {
            boolean issue = entry.getKey();
            List<FilterDataWithSender> filters = entry.getValue();

            //System.out.println("\nProcessing issue = " + issue);

            // Sort intervals by start time
            filters.sort(Comparator.comparingInt(fws -> fws.filterData.getStart()));
            //System.out.println("Sorted intervals: " + filters.stream().map(fws -> fws.filterData.toString()).collect(Collectors.toList()));

            // Merge intervals and track senders
            List<MergedInterval> mergedIntervals = mergeIntervals(filters);
            combinedResults.addAll(mergedIntervals);

            // Print results
            //System.out.println("Merged intervals:");
            for (MergedInterval interval : mergedIntervals) {
                //System.out.println(interval);
            }
        }

        // Sort the combined results by start time
        combinedResults.sort(Comparator.comparingInt(MergedInterval::getStart));

        // Print combined results
        //System.out.println("\nAll merged intervals combined:");
        for (MergedInterval interval : combinedResults) {
            //System.out.println(interval);
        }

        return combinedResults;
    }


    // Helper class to associate FilterData with its sender
    private static class FilterDataWithSender {
        FilterData filterData;
        DTNHost sender;

        public FilterDataWithSender(FilterData filterData, DTNHost sender) {
            this.filterData = filterData;
            this.sender = sender;
        }
    }

    private List<MergedInterval> mergeIntervals(List<FilterDataWithSender> filtersWithSenders) {
        if (filtersWithSenders.isEmpty()) {
            return Collections.emptyList();
        }

        // Create a list of all interval points (start and end)
        List<IntervalPoint> points = new ArrayList<>();
        for (FilterDataWithSender fws : filtersWithSenders) {
            points.add(new IntervalPoint(fws.filterData.getStart(), true, fws.sender));
            points.add(new IntervalPoint(fws.filterData.getEnd(), false, fws.sender));
        }

        // Sort points
        Collections.sort(points);

        List<MergedInterval> result = new ArrayList<>();
        Set<DTNHost> activeSenders = new HashSet<>();
        int prevPoint = -1;

        for (IntervalPoint point : points) {
            if (!activeSenders.isEmpty() && prevPoint != point.value) {
                // Add the interval from prevPoint to current point
                result.add(new MergedInterval(prevPoint, point.value, new HashSet<>(activeSenders)));
            }

            if (point.isStart) {
                activeSenders.add(point.sender);
            } else {
                activeSenders.remove(point.sender);
            }

            prevPoint = point.value;
        }

        return result;
    }

    // IntervalPoint and MergedInterval classes remain the same as before
    private static class IntervalPoint implements Comparable<IntervalPoint> {
        int value;
        boolean isStart;
        DTNHost sender;

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
    }

    public static class MergedInterval {
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
}

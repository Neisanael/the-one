package report;

import GroupBased.IKeyListener;
import GroupBased.Model.MergedInterval;
import GroupBased.Model.PairKey;
import core.DTNHost;
import core.GroupBased.*;
import core.Message;
import core.SimClock;
import core.UpdateListener;

import javax.crypto.SecretKey;
import java.util.*;

public class KeysPerHostReport extends Report implements IKeyListener, UpdateListener {
    private Map<DTNHost, List<SecretKey>> pairKeysPerHost;
    private Map<DTNHost, List<SecretKey>> groupKeysPerBroker;
    private Map<DTNHost, List<SecretKey>> messageDecrypted;
    private Map<DTNHost, List<MergedInterval>> groupsPerHost;
    private Set<Message> eventsCreated;
    private Set<Message> filtersCreated;
    private Set<Message> encryptedEventsCreated;
    private List<Set<Message>> generationMsg;


    private double lastUpdate = 0; // Last update time
    private final double timeThreshold = 360; // Update threshold in simulation time

    // Cost tracking
    private int totalComputingCostGroup = 0; // Total computing cost for PSGuard (ms)
    private int totalNetworkingCostGroup = 0; // Total networking cost for PSGuard (KB)

    private int subscriberCount = 0; // Total number of unique subscribers
    private final Set<DTNHost> countedSubscribers = new HashSet<>(); // Track unique subscribers

    public KeysPerHostReport() {
        init();
    }

    protected void init(){
        super.init();
        this.pairKeysPerHost = new HashMap<>();
        this.groupKeysPerBroker = new HashMap<>();
        this.messageDecrypted = new HashMap<>();
        this.groupsPerHost = new HashMap<>();
        this.eventsCreated = new HashSet<>();
        this.filtersCreated = new HashSet<>();
        this.encryptedEventsCreated = new HashSet<>();
        this.generationMsg = new ArrayList<>();
    }

    @Override
    public void eventsCreated(Message event) {
        this.eventsCreated.add(event);
    }

    @Override
    public void filtersCreated(Message filter) {
        this.filtersCreated.add(filter);
    }

    @Override
    public void encryptedEventsCreated(Message encryptedEvent) {
        this.encryptedEventsCreated.add(encryptedEvent);
    }

    @Override
    public void groupKeyGeneratedByBroker(SecretKey key, DTNHost broker) {
        this.groupKeysPerBroker.computeIfAbsent(broker, k -> new ArrayList<>()).add(key);
    }

    @Override
    public void openedMessage(SecretKey key, DTNHost subscriber) {
        this.messageDecrypted.computeIfAbsent(subscriber, k -> new ArrayList<>()).add(key);
    }

    @Override
    public void generatedGroups(DTNHost maker, MergedInterval mergedInterval) {
        this.groupsPerHost.computeIfAbsent(maker, k -> new ArrayList<>()).add(mergedInterval);
    }

    @Override
    public void keyPairCreated(PairKey pairKey) {
        if(!this.pairKeysPerHost.containsKey(pairKey.getHostThisKeyBelongsTo())){
            List<SecretKey> keys = new ArrayList<>();
            keys.add(pairKey.getSecretKey());
            this.pairKeysPerHost.put(pairKey.getHostThisKeyBelongsTo(), keys);
        }else{
            this.pairKeysPerHost.computeIfAbsent(pairKey.getHostThisKeyBelongsTo(), k -> new ArrayList<>()).add(pairKey.getSecretKey());
        }
    }

    @Override
    public void generationLoad(Set<Message> messages) {
        generationMsg.add(messages);
    }

    public int totalEventsCreated(){
        return this.eventsCreated.size();
    }

    public int totalFiltersCreated(){
        return this.filtersCreated.size();
    }

    public int totalEncryptedEventsCreated(){
        return this.encryptedEventsCreated.size();
    }

    public int totalPairKeys(){
        return this.pairKeysPerHost.values().stream()
                .mapToInt(List::size)
                .sum();
    }

    public int totalPairKeysByHostType(Class<?> hostType) {
        return this.pairKeysPerHost.entrySet().stream()
                .filter(entry -> hostType.isInstance(entry.getKey()))
                .mapToInt(entry -> entry.getValue().size())
                .sum();
    }

    public int totalPublisherPairKeys() {
        return totalPairKeysByHostType(Publisher.class);
    }

    public int totalSubscriberPairKeys() {
        return totalPairKeysByHostType(Subscriber.class);
    }


    public int totalGroupsPerHost(){
        return this.groupsPerHost.values().stream()
                .mapToInt(List::size)
                .sum();
    }

    public int totalMessageOpened(){
        return messageDecrypted.values().stream()
                .mapToInt(List::size)
                .sum();
    }

    public int totalGroupKeysPerBroker(){
        return this.groupKeysPerBroker.values().stream()
                .mapToInt(List::size)
                .sum();
    }

    public int totalGenerationLoad() {
        return generationMsg.stream()
                .flatMap(Set::stream)
                .mapToInt(Message::getSize)
                .sum();
    }

    public int totalGenerationDoes(){
        return generationMsg.size();
    }

    public int avgComputingCostGroup(){
        return subscriberCount > 0
                ? totalComputingCostGroup / subscriberCount
                : 0;
    }

    @Override
    public void done() {
        write("Message stats for scenario " + getScenarioName());

        String statsText = "\ncreatedBrokerKeys : " + this.totalGroupKeysPerBroker() +
                "\nopenedMessages : " + this.totalMessageOpened() +
                "\ncreatedPairKey : " + this.totalPairKeys() +
                "\ncreatedGroup : " + this.totalGroupsPerHost() +
                "\ncreatedPairKeyBySubscriber : " + this.totalSubscriberPairKeys() +
                "\ncreatedPairKeyByPublisher : " + this.totalPublisherPairKeys() +
                "\ntotalCreatedEvents : " + this.totalEventsCreated() +
                "\ntotalCreatedFilters : " + this.totalFiltersCreated() +
                "\ntotalCreatedEncryptedEvents : " + this.totalEncryptedEventsCreated() +
                "\ntotalSizeWhenMsgGrouped : " + this.totalGenerationLoad() +
                "\ntotalHowManyBrokerGenerateGroup : " + this.totalGenerationDoes() +
                "\ntotalComputingCost(ms) : " + this.totalComputingCostGroup +
                "\navgComputingCostPerSubscriber(ms) : " + this.avgComputingCostGroup() +
                "\ntotalSubscriber : " + this.subscriberCount
                ;

        write(statsText);
        super.done();
    }

    @Override
    public void updated(List<DTNHost> hosts) {
        if (isWarmup()) {
            return; // Skip updates during warm-up phase
        }

        // Current simulation time
        double currentTime = SimClock.getTime();

        // Update only if threshold time has passed
        if ((currentTime - lastUpdate) < timeThreshold) {
            return;
        }
        lastUpdate = currentTime;

        // Iterate through all hosts to calculate cost for PSGuard
        for (DTNHost host : hosts) {
            if (host != null) {
                // Simulate the addition of a new subscriber
                if (!countedSubscribers.contains(host) && host instanceof Subscriber) {
                    countedSubscribers.add(host); // Mark this host as counted
                    subscriberCount++; // Increment total unique subscribers
                }
                if (host instanceof Broker) {
                    // Compute costs for PSGuard using buffer and free space
                    computePSGuardCostWithBuffer(host);
                }
            }
        }
    }

    /**
     * Computes PSGuard costs using remaining buffer size dynamically.
     */
    private void computePSGuardCostWithBuffer(DTNHost host) {
        // Compute the dynamically calculated computing cost
        int usedBuffer = (int)(host.getRouter().getFreeBufferSize() - host.getRouter().getFreeBufferSize()); // Used buffer size in bytes
        int dynamicComputingCost = calculateDynamicComputingCost(usedBuffer); // Determine cost based on buffer usage
        totalComputingCostGroup += dynamicComputingCost;

        // Networking cost: based on buffer usage
        totalNetworkingCostGroup += usedBuffer / 1024; // Convert buffer size from bytes to KB
    }

    /**
     * Calculates dynamic computing cost based on buffer usage.
     *
     * @param usedBuffer The size of the currently used buffer (in bytes).
     * @return The dynamic computing cost (in ms).
     */
    private int calculateDynamicComputingCost(int usedBuffer) {
        // Example: Computing cost increases logarithmically with buffer usage
        int baseCost = 1; // Base cost for computation in ms
        int calculatedCost = (int) (baseCost + Math.log(1 + usedBuffer / 1024.0) * 10);
        return Math.max(calculatedCost, baseCost); // Ensure cost is at least base cost
    }

}

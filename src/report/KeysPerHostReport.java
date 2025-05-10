package report;

import core.DTNHost;
import core.GroupBased.*;
import core.Message;

import javax.crypto.SecretKey;
import java.util.*;

public class KeysPerHostReport extends Report implements IKeyListener {
    private Map<DTNHost, List<SecretKey>> pairKeysPerHost;
    private Map<DTNHost, List<SecretKey>> groupKeysPerBroker;
    private Map<DTNHost, List<SecretKey>> messageDecrypted;
    private Map<DTNHost, List<MergedInterval>> groupsPerHost;
    private Set<Message> eventsCreated;
    private Set<Message> filtersCreated;
    private Set<Message> encryptedEventsCreated;

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
                "\ntotalCreatedEncryptedEvents : " + this.totalEncryptedEventsCreated()
                ;

        write(statsText);
        super.done();
    }
}

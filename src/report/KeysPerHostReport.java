package report;

import core.DTNHost;
import core.GroupBased.*;

import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KeysPerHostReport extends Report implements IKeyListener {
    private Map<DTNHost, List<SecretKey>> pairKeysPerHost;
    private Map<DTNHost, List<SecretKey>> groupKeysPerBroker;
    private Map<DTNHost, List<SecretKey>> groupKeysPerSubscriber;
    private Map<DTNHost, List<MergedInterval>> groupsPerHost;

    public KeysPerHostReport() {
        init();
    }

    protected void init(){
        super.init();
        pairKeysPerHost = new HashMap<>();
        groupKeysPerBroker = new HashMap<>();
        groupKeysPerSubscriber = new HashMap<>();
        groupsPerHost = new HashMap<>();
    }

    @Override
    public void groupKeyGeneratedByBroker(SecretKey key, DTNHost broker) {
        groupKeysPerBroker.computeIfAbsent(broker, k -> new ArrayList<>()).add(key);
    }

    @Override
    public void groupKeyGeneratedBySubscriber(SecretKey key, DTNHost subscriber) {
        groupKeysPerSubscriber.computeIfAbsent(subscriber, k -> new ArrayList<>()).add(key);
    }

    @Override
    public void generatedGroups(DTNHost maker, MergedInterval mergedInterval) {
        System.out.println(groupsPerHost);
        System.out.println(maker);
        groupsPerHost.computeIfAbsent(maker, k -> new ArrayList<>()).add(mergedInterval);
    }

    @Override
    public void keyPairCreated(PairKey pairKey) {
        if(!pairKeysPerHost.containsKey(pairKey.getHostThisKeyBelongsTo())){
            List<SecretKey> keys = new ArrayList<>();
            keys.add(pairKey.getSecretKey());
            pairKeysPerHost.put(pairKey.getHostThisKeyBelongsTo(), keys);
        }else{
            pairKeysPerHost.computeIfAbsent(pairKey.getHostThisKeyBelongsTo(), k -> new ArrayList<>()).add(pairKey.getSecretKey());
        }
    }

    public int totalPairKeys(){
        return pairKeysPerHost.values().stream()
                .mapToInt(List::size)
                .sum();
    }

    public int totalPairKeysByHostType(Class<?> hostType) {
        return pairKeysPerHost.entrySet().stream()
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
        return groupsPerHost.values().stream()
                .mapToInt(List::size)
                .sum();
    }

    public int totalGroupKeysPerSubscriber(){
        return groupKeysPerSubscriber.values().stream()
                .mapToInt(List::size)
                .sum();
    }

    public int totalGroupKeysPerBroker(){
        return groupKeysPerBroker.values().stream()
                .mapToInt(List::size)
                .sum();
    }

    @Override
    public void done() {
        write("Message stats for scenario " + getScenarioName());

        String statsText = "\ncreatedBrokerKeys : " + this.totalGroupKeysPerBroker() +
                "\ncreatedSubscriberKeys : " + this.totalGroupKeysPerSubscriber() +
                "\ncreatedPairKey : " + this.totalPairKeys() +
                "\ncreatedGroup : " + this.totalGroupsPerHost() +
                "\ncreatedPairKeyBySubscriber : " + this.totalSubscriberPairKeys() +
                "\ncreatedPairKeyByPublisher : " + this.totalPublisherPairKeys()
                ;

        write(statsText);
        super.done();
    }
}

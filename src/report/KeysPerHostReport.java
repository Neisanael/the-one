package report;

import core.DTNHost;
import core.GroupBased.IKeyListener;
import core.GroupBased.MergedInterval;
import core.GroupBased.PairKey;

import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KeysPerHostReport extends Report implements IKeyListener {
    private Map<DTNHost, List<SecretKey>> pairKeysPerHost;
    private Map<DTNHost, List<SecretKey>> groupKeysPerBroker;
    private Map<DTNHost, List<SecretKey>> groupKeysPerSubscriber;
    private Map<DTNHost, List<MergedInterval>> totalGroupsPerHost;

    public KeysPerHostReport() {
        init();
    }

    protected void init(){
        super.init();
        pairKeysPerHost = new HashMap<>();
        groupKeysPerBroker = new HashMap<>();
        groupKeysPerSubscriber = new HashMap<>();
        totalGroupsPerHost = new HashMap<>();
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
        totalGroupsPerHost.computeIfAbsent(maker, k -> new ArrayList<>()).add(mergedInterval);
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

    @Override
    public void done() {
        write("Message stats for scenario " + getScenarioName());

        String statsText = "\ncreatedBrokerKeys : " + this.groupKeysPerBroker +
                "\ncreatedSubscriberKeys : " + this.groupKeysPerSubscriber +
                "\ncreatedPairKey : " + this.pairKeysPerHost +
                "\ncreatedGroup : " + this.totalGroupsPerHost
                ;

        write(statsText);
        super.done();
    }
}

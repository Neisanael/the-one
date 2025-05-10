package core.GroupBased;

import core.DTNHost;

import javax.crypto.SecretKey;

public interface IKeyListener {

    public void groupKeyGeneratedByBroker(SecretKey key, DTNHost broker);

    public void openedMessage(SecretKey key, DTNHost subscriber);

    public void generatedGroups(DTNHost maker, MergedInterval mergedInterval);

    public void keyPairCreated(PairKey pairKey);
}

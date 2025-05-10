package core.GroupBased;

import core.DTNHost;
import core.Message;

import javax.crypto.SecretKey;

public interface IKeyListener {

    public void eventsCreated(Message event);

    public void filtersCreated(Message filter);

    public void encryptedEventsCreated(Message encryptedEvent);

    public void groupKeyGeneratedByBroker(SecretKey key, DTNHost broker);

    public void openedMessage(SecretKey key, DTNHost subscriber);

    public void generatedGroups(DTNHost maker, MergedInterval mergedInterval);

    public void keyPairCreated(PairKey pairKey);
}

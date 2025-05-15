package GroupBased;

import GroupBased.Model.MergedInterval;
import GroupBased.Model.PairKey;
import core.DTNHost;
import core.Message;

import javax.crypto.SecretKey;
import java.util.List;
import java.util.Set;

public interface IKeyListener {

    public void eventsCreated(Message event);

    public void filtersCreated(Message filter);

    public void encryptedEventsCreated(Message encryptedEvent);

    public void groupKeyGeneratedByBroker(SecretKey key, DTNHost broker);

    public void openedMessage(SecretKey key, DTNHost subscriber);

    public void latenciesGroupKey(double latency);

    public void generatedGroups(DTNHost maker, List<MergedInterval> mergedInterval);

    public void keyPairCreated(PairKey pairKey);

    public void generationLoad(Set<Message> messages);
}

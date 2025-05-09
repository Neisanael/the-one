package input;

import GroupBased.GenerateInterest;
import GroupBased.LoremIpsumGenerator;
import GroupBased.PropertySettings;
import core.DTNHost;
import core.GroupBased.Broker;
import core.GroupBased.PairKey;
import core.GroupBased.Publisher;
import core.GroupBased.Subscriber;
import core.Message;
import core.World;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

public class MessageCreateEventGroup extends MessageEvent implements PropertySettings {
    private final int size;
    private final int responseSize;

    /**
     * Creates a message creation event with a optional response request
     * @param from The creator of the message
     * @param to Where the message is destined to
     * @param id ID of the message
     * @param size Size of the message
     * @param responseSize Size of the requested response message or 0 if
     * no response is requested
     * @param time Time, when the message is created
     */
    public MessageCreateEventGroup(int from, int to, String id, int size,
                                   int responseSize, double time) {
        super(from,to, id, time);
        this.size = size;
        this.responseSize = responseSize;
    }


    /**
     * Creates the message this event represents.
     */
    @Override
    public void processEvent(World world) {
        DTNHost to = world.getNodeByAddress(this.toAddr);
        DTNHost from = world.getNodeByAddress(this.fromAddr);
        if(from instanceof Publisher && to instanceof Broker){
            if(((Publisher) from).getPairKey().getSecretKey() != null){
                Message m = new Message(from, to, this.id, this.size);
                m.addProperty(EVENTS, GenerateInterest.generateEventData());
                m.setResponseSize(this.responseSize);
                from.createNewMessage(m);
            }
        } else if (from instanceof Subscriber && to instanceof Broker){
            if(((Subscriber) from).getPairKey().getSecretKey() != null){
                Message m = new Message(from, to, this.id, this.size);
                m.addProperty(FILTERS, GenerateInterest.generateFilterData());
                m.setResponseSize(this.responseSize);
                from.createNewMessage(m);
            }
        } else if (from instanceof Broker && to instanceof Publisher){
            if(!((Broker) from).getEncryptedEventsGrouped().isEmpty()){
                Message m = new Message(from, to, this.id, this.size);
                m.addProperty(ENCRYPTED, ((Broker) from).getEncryptedEventsGrouped());
                m.setResponseSize(this.responseSize);
                from.createNewMessage(m);
            }
        }
    }

    @Override
    public String toString() {
        return super.toString() + " [" + fromAddr + "->" + toAddr + "] " +
                "size:" + size + " CREATE";
    }
}

package input.GroupBased;

import GroupBased.GenerateInterest;
import GroupBased.PropertySettings;
import core.DTNHost;
import core.GroupBased.Broker;
import core.GroupBased.Publisher;
import core.GroupBased.Subscriber;
import core.Message;
import core.World;
import input.MessageEvent;

public class MessageCreateEncryptedBroker extends MessageEvent implements PropertySettings {
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
    public MessageCreateEncryptedBroker(int from, int to, String id, int size,
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
        if (from instanceof Broker){
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

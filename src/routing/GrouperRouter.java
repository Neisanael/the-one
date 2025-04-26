package routing;

import GroupBased.PropertySettings;
import core.Connection;
import core.DTNHost;
import core.GroupBased.Broker;
import core.GroupBased.Publisher;
import core.GroupBased.Subscriber;
import core.Message;
import core.Settings;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GrouperRouter extends ActiveRouter implements PropertySettings {
    public GrouperRouter(Settings s) {
        super(s);
    }

    protected GrouperRouter(GrouperRouter r) {
        super(r);
    }

    @Override
    public void changedConnection(Connection con) {
        super.changedConnection(con);
        if (con.isUp()) {
            DTNHost otherHost = con.getOtherNode(getHost());
            try {
                exchangeKeysWith(otherHost);
                updateKeysWith(otherHost);
            } catch (Exception e) {
                //System.err.println("DH key exchange failed with " + otherHost);
            }
        }
    }

    private void exchangeKeysWith(DTNHost otherHost) throws Exception {
        SecureRandom random = new SecureRandom();
        int bitLength = 512;

        // Generate large prime modulus and generator
        BigInteger primeP = BigInteger.probablePrime(bitLength, random);
        BigInteger primeG = BigInteger.probablePrime(bitLength, random);

        // Generate private keys
        BigInteger privBroker = new BigInteger(256, random);
        BigInteger privSubscriber = new BigInteger(256, random);

        // Compute public keys
        BigInteger pubBroker = primeG.modPow(privBroker, primeP);
        BigInteger pubSubscriber = primeG.modPow(privSubscriber, primeP);
        if (this.getHost() instanceof Broker && otherHost instanceof Subscriber) {
            if(((Broker) this.getHost()).getPublicSecretKey().containsKey(otherHost)){
                return;
            }else {
                BigInteger secretA = pubSubscriber.modPow(privBroker, primeP); // B^a mod p
                ((Subscriber) otherHost).addPublicSecretKey(this.getHost(), secretA);
                BigInteger secretB = pubBroker.modPow(privSubscriber, primeP); // A^b mod p
                ((Broker) this.getHost()).addPublicSecretKey(otherHost, secretB);
            }
        }else if(this.getHost() instanceof Subscriber && otherHost instanceof Broker){
            if(((Subscriber) this.getHost()).getPublicSecretKey().containsKey(otherHost)){
                return;
            }else{
                BigInteger secretA = pubSubscriber.modPow(privBroker, primeP); // B^a mod p
                ((Subscriber) this.getHost()).addPublicSecretKey(otherHost, secretA);
                BigInteger secretB = pubBroker.modPow(privSubscriber, primeP); // A^b mod p
                ((Broker) otherHost).addPublicSecretKey(this.getHost(), secretB);
            }
        }
    }

    private void updateKeysWith(DTNHost otherHost) throws Exception {
        if(otherHost instanceof Broker && this.getHost() instanceof Broker){
            for (Map.Entry<DTNHost, BigInteger> entry : ((Broker) otherHost).getPublicSecretKey().entrySet()) {
                if (!((Broker) this.getHost()).getPublicSecretKey().containsKey(entry.getKey())) {
                    ((Broker) this.getHost()).addPublicSecretKey(entry.getKey(), entry.getValue());
                }
            }
            for (Map.Entry<DTNHost, BigInteger> entry : ((Broker) this.getHost()).getPublicSecretKey().entrySet()) {
                if (!((Broker) otherHost).getPublicSecretKey().containsKey(entry.getKey())) {
                    ((Broker) otherHost).addPublicSecretKey(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    /**
     * Drops messages whose TTL is less than zero.
     */
    protected void dropExpiredMessages() {
        Message[] messages = getMessageCollection().toArray(new Message[0]);
        for (Message message : messages) {
            int ttl = message.getTtl();
            if (ttl <= 0) {
                // TODO : grouping because in message collection there is message deleted
                if(this.getHost() instanceof Broker) {
                    ((Broker) this.getHost()).makeGroups();
                }
                deleteMessage(message.getId(), true);
            }
        }
    }

    /**
     * Informs the host that a message was successfully transferred.
     * @param id Identifier of the message
     * @param from From who the message was from
     */
    @Override
    public Message messageTransferred(String id, DTNHost from) {
        Message msg = super.messageTransferred(id, from);
        if (this.getHost() instanceof Broker) {
            ((Broker) this.getHost()).makeGroups();
        }
        return msg;
    }

    @Override
    public boolean createNewMessage(Message msg) {
        makeRoomForNewMessage(msg.getSize());
        msg.setTtl(this.msgTtl);
        //addToMessages(msg, !(msg.getFrom() instanceof Subscriber) && !(msg.getFrom() instanceof Publisher));
        addToMessages(msg, true);
        return true;
    }

    @Override
    public void update() {
        super.update();
        if (isTransferring() || !canStartTransfer()) {
            return; // transferring, don't try other connections yet
        }

        // Try first the messages that can be delivered to final recipient
        if (exchangeDeliverableMessages() != null) {
            return; // started a transfer, don't try others (yet)
        }

        if(this.getHost() instanceof Broker){
            if(!getConnectedBrokerOrSubscriberHosts().isEmpty()){
                this.tryMessagesToConnections(getMessagesWithEncrypted(), getConnectedBrokerOrSubscriberHosts());
            }
        }else if(this.getHost() instanceof Subscriber){
            if(!getConnectedBrokerOrSubscriberHosts().isEmpty()){
                this.tryMessagesToConnections(getMessagesWithFilters(), getConnectedBrokerOrSubscriberHosts());
            }
        }else if(this.getHost() instanceof Publisher){
            if(!getConnectedBrokerOrPublisherHosts().isEmpty()){
                this.tryMessagesToConnections(getMessagesWithEvents(), getConnectedBrokerOrPublisherHosts());
            }
        }
        // then try other message
    }

    protected List<Connection> getConnectedBrokerOrSubscriberHosts() {
        List<Connection> list = new ArrayList<Connection>();
        for (Connection connection : this.getHost().getConnections()) {
            if(connection.getOtherNode(this.getHost()) instanceof Broker || connection.getOtherNode(this.getHost()) instanceof Subscriber){
                list.add(connection);
            }
        }
        return list;
    }

    protected List<Connection> getConnectedBrokerOrPublisherHosts(){
        List<Connection> list = new ArrayList<Connection>();
        for (Connection connection : this.getHost().getConnections()) {
            if(connection.getOtherNode(this.getHost()) instanceof Broker || connection.getOtherNode(this.getHost()) instanceof Publisher){
                list.add(connection);
            }
        }
        return list;
    }

    /**
     * Creates and returns a list of messages this router is currently
     * carrying and still has copies left to distribute (nrof copies > 1).
     * @return A list of messages that have copies left
     */
    protected List<Message> getMessagesWithEvents() {
        List<Message> list = new ArrayList<Message>();

        for (Message m : getMessageCollection()) {
            if(m.getProperty(EVENTS) != null){
                list.add(m);
            }
        }

        return list;
    }

    protected List<Message> getMessagesWithEncrypted() {
        List<Message> list = new ArrayList<Message>();

        for (Message m : getMessageCollection()) {
            if(m.getProperty(ENCRYPTED) != null) {
                list.add(m);
            }
        }

        return list;
    }

    protected List<Message> getMessagesWithFilters() {
        List<Message> list = new ArrayList<Message>();

        for (Message m : getMessageCollection()) {
            if(m.getProperty(FILTERS) != null) {
                list.add(m);
            }
        }

        return list;
    }

    @Override
    public GrouperRouter replicate() {
        return new GrouperRouter(this);
    }
}

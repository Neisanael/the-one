package routing;

import GroupBased.PropertySettings;
import core.*;
import core.GroupBased.Broker;
import core.GroupBased.PairKey;
import core.GroupBased.Publisher;
import core.GroupBased.Subscriber;
import routing.util.SubscriberKey;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;

public class GrouperRouter extends ActiveRouter implements PropertySettings {
    public GrouperRouter(Settings s) {
        super(s);
        if (s.contains(MSG_TTL_S)) {
            this.msgTtl = s.getInt(MSG_TTL_S);

            if (this.msgTtl > MAX_TTL_VALUE){
                throw new SettingsError("Invalid value for " +
                        s.getFullPropertyName(MSG_TTL_S) +
                        ". Max value is limited to "+MAX_TTL_VALUE);
            }
        }
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
        BigInteger privSubscriberOrPublisher = new BigInteger(256, random);

        // Compute public keys
        BigInteger pubBroker = primeG.modPow(privBroker, primeP);
        BigInteger pubSubscriberOrPublisher = primeG.modPow(privSubscriberOrPublisher, primeP);
        boolean isBrokerHasSubscriberOrPublisherPairKey = false;
        boolean isKeyEndTime = false;
        if (this.getHost() instanceof Broker && otherHost instanceof Subscriber) {
            for(PairKey pairKey : ((Broker) this.getHost()).getPairKeys()){
                if (((Subscriber) otherHost).getPairKey().getSecretKey() == pairKey.getSecretKey()) {
                    isBrokerHasSubscriberOrPublisherPairKey = true;
                    double totalCreatedSecretKeyWithMsgTtl = ((Subscriber) otherHost).getPairKey().getTimeSecretKeyCreated() + this.getMsgTtl();
                    if(totalCreatedSecretKeyWithMsgTtl == SimClock.getTime()){
                        isKeyEndTime = true;
                    }
                    break;
                }
            }
            if(!isBrokerHasSubscriberOrPublisherPairKey || isKeyEndTime){
                //Add Key to subscriber
                BigInteger secretA = pubSubscriberOrPublisher.modPow(privBroker, primeP); // B^a mod p
                //((Subscriber) otherHost).addPublicSecretKey(this.getHost(), convertToAESKey(secretA));
                PairKey subscriberPairKey = new PairKey();
                subscriberPairKey.setSecretKey(convertToAESKey(secretA));
                subscriberPairKey.setHostThisKeyBelongsTo(otherHost);
                subscriberPairKey.setTimeSecretKeyCreated(SimClock.getTime());
                ((Subscriber) otherHost).setPairKey(subscriberPairKey);
                //Add Key to broker
                BigInteger secretB = pubBroker.modPow(privSubscriberOrPublisher, primeP); // A^b mod p
                //((Broker) this.getHost()).addPublicSecretKey(otherHost, convertToAESKey(secretB));
                PairKey brokerPairKey = new PairKey();
                brokerPairKey.setSecretKey(convertToAESKey(secretB));
                brokerPairKey.setHostThisKeyBelongsTo(otherHost);
                brokerPairKey.setTimeSecretKeyCreated(SimClock.getTime());
                ((Broker) this.getHost()).addPairKey(brokerPairKey);
            }
        }else if(this.getHost() instanceof Subscriber && otherHost instanceof Broker){
            for(PairKey pairKey : ((Broker) otherHost).getPairKeys()){
                if (((Subscriber) this.getHost()).getPairKey().getSecretKey() == pairKey.getSecretKey()) {
                    isBrokerHasSubscriberOrPublisherPairKey = true;
                    double totalCreatedSecretKeyWithMsgTtl = ((Subscriber) this.getHost()).getPairKey().getTimeSecretKeyCreated() + this.getMsgTtl();
                    if(totalCreatedSecretKeyWithMsgTtl == SimClock.getTime()){
                        isKeyEndTime = true;
                    }
                    break;
                }
            }
            if(!isBrokerHasSubscriberOrPublisherPairKey || isKeyEndTime){
                //Add Key to subscriber
                BigInteger secretA = pubSubscriberOrPublisher.modPow(privBroker, primeP); // B^a mod p
                //((Subscriber) this.getHost()).addPublicSecretKey(otherHost, convertToAESKey(secretA));
                PairKey subscriberPairKey = new PairKey();
                subscriberPairKey.setSecretKey(convertToAESKey(secretA));
                subscriberPairKey.setHostThisKeyBelongsTo(this.getHost());
                subscriberPairKey.setTimeSecretKeyCreated(SimClock.getTime());
                ((Subscriber) this.getHost()).setPairKey(subscriberPairKey);
                //Add Key to broker
                BigInteger secretB = pubBroker.modPow(privSubscriberOrPublisher, primeP); // A^b mod p
                //((Broker) otherHost).addPublicSecretKey(this.getHost(), convertToAESKey(secretB));
                PairKey brokerPairKey = new PairKey();
                brokerPairKey.setSecretKey(convertToAESKey(secretB));
                brokerPairKey.setHostThisKeyBelongsTo(this.getHost());
                brokerPairKey.setTimeSecretKeyCreated(SimClock.getTime());
                ((Broker) otherHost).addPairKey(brokerPairKey);
            }
        }else if(this.getHost() instanceof Broker && otherHost instanceof Publisher){
            for(PairKey pairKey : ((Broker) this.getHost()).getPairKeys()){
                if (((Publisher) otherHost).getPairKey().getSecretKey() == pairKey.getSecretKey()) {
                    isBrokerHasSubscriberOrPublisherPairKey = true;
                    double totalCreatedSecretKeyWithMsgTtl = ((Publisher) otherHost).getPairKey().getTimeSecretKeyCreated() + this.getMsgTtl();
                    if(totalCreatedSecretKeyWithMsgTtl == SimClock.getTime()){
                        isKeyEndTime = true;
                    }
                    break;
                }
            }
            if(!isBrokerHasSubscriberOrPublisherPairKey || isKeyEndTime){
                //Add Key to publisher
                BigInteger secretA = pubSubscriberOrPublisher.modPow(privBroker, primeP); // B^a mod p
                //((Publisher) otherHost).addPublicSecretKey(otherHost, convertToAESKey(secretA));
                PairKey publisherPairKey = new PairKey();
                publisherPairKey.setSecretKey(convertToAESKey(secretA));
                publisherPairKey.setHostThisKeyBelongsTo(otherHost);
                publisherPairKey.setTimeSecretKeyCreated(SimClock.getTime());
                ((Publisher) otherHost).setPairKey(publisherPairKey);
                //Add Key to broker
                BigInteger secretB = pubBroker.modPow(privSubscriberOrPublisher, primeP); // A^b mod p
                //((Broker) this.getHost()).addPublicSecretKey(otherHost, convertToAESKey(secretB));
                PairKey brokerPairKey = new PairKey();
                brokerPairKey.setSecretKey(convertToAESKey(secretB));
                brokerPairKey.setHostThisKeyBelongsTo(otherHost);
                brokerPairKey.setTimeSecretKeyCreated(SimClock.getTime());
                ((Broker) this.getHost()).addPairKey(brokerPairKey);
            }
        }else if(this.getHost() instanceof Publisher && otherHost instanceof Broker){
            for(PairKey pairKey : ((Broker) this.getHost()).getPairKeys()){
                if (((Publisher) this.getHost()).getPairKey().getSecretKey() == pairKey.getSecretKey()) {
                    isBrokerHasSubscriberOrPublisherPairKey = true;
                    double totalCreatedSecretKeyWithMsgTtl = ((Publisher) this.getHost()).getPairKey().getTimeSecretKeyCreated() + this.getMsgTtl();
                    if(totalCreatedSecretKeyWithMsgTtl == SimClock.getTime()){
                        isKeyEndTime = true;
                    }
                    break;
                }
            }
            if(!isBrokerHasSubscriberOrPublisherPairKey || isKeyEndTime){
                //Add Key to publisher
                BigInteger secretA = pubSubscriberOrPublisher.modPow(privBroker, primeP); // B^a mod p
                //((Publisher) this.getHost()).addPublicSecretKey(otherHost, convertToAESKey(secretA));
                PairKey publisherPairKey = new PairKey();
                publisherPairKey.setSecretKey(convertToAESKey(secretA));
                publisherPairKey.setHostThisKeyBelongsTo(otherHost);
                publisherPairKey.setTimeSecretKeyCreated(SimClock.getTime());
                ((Publisher) this.getHost()).setPairKey(publisherPairKey);
                //Add Key to broker
                BigInteger secretB = pubBroker.modPow(privSubscriberOrPublisher, primeP); // A^b mod p
                //((Broker) otherHost).addPublicSecretKey(otherHost, convertToAESKey(secretB));
                PairKey brokerPairKey = new PairKey();
                brokerPairKey.setSecretKey(convertToAESKey(secretB));
                brokerPairKey.setHostThisKeyBelongsTo(otherHost);
                brokerPairKey.setTimeSecretKeyCreated(SimClock.getTime());
                ((Broker) otherHost).addPairKey(brokerPairKey);
            }
        }
    }

    private void updateKeysWith(DTNHost otherHost) throws Exception {
        if(otherHost instanceof Broker && this.getHost() instanceof Broker){
            for (PairKey otherPairKey : ((Broker) otherHost).getPairKeys()) {
                boolean found = false;
                for (PairKey pairKey : ((Broker) this.getHost()).getPairKeys()) {
                    if (pairKey.getSecretKey().equals(otherPairKey.getSecretKey())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    ((Broker) this.getHost()).addPairKey(otherPairKey);
                }
            }
            for (PairKey pairKey : ((Broker) this.getHost()).getPairKeys()) {
                boolean found = false;
                for (PairKey otherPairKey : ((Broker) otherHost).getPairKeys()) {
                    if (pairKey.getSecretKey().equals(otherPairKey.getSecretKey())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    ((Broker) otherHost).addPairKey(pairKey);
                }
            }
        }
    }

    public static SecretKey convertToAESKey(BigInteger bigInt) throws NoSuchAlgorithmException {
        byte[] keyBytes = bigInt.toByteArray();
        byte[] normalizedKey = new byte[32];
        int length = Math.min(keyBytes.length, 32);
        System.arraycopy(keyBytes, 0, normalizedKey, 32 - length, length);
        return new SecretKeySpec(normalizedKey, "AES");
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

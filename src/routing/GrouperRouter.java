package routing;

import GroupBased.IKeyListener;
import GroupBased.Model.KeyCache;
import GroupBased.Model.PairKey;
import GroupBased.PropertySettings;
import core.*;
import core.GroupBased.*;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class GrouperRouter extends ActiveRouter implements PropertySettings {

    private List<IKeyListener> kListeners;

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
        this.kListeners = r.kListeners;
    }

    @Override
    public void init(DTNHost host, List<MessageListener> mListeners, List<IKeyListener> kListeners) {
        super.init(host, mListeners, kListeners);
        this.kListeners = kListeners;
    }

    /*@Override
    public int receiveMessage(Message m, DTNHost from) {
        if(m.getFrom() instanceof Broker && this.getHost().equals(m.getTo()) ){
            return DENIED_UNSPECIFIED;
        }else if(m.getFrom() instanceof Subscriber && this.getHost().equals(m.getTo())){
            return DENIED_UNSPECIFIED;
        }else if(m.getFrom() instanceof Publisher && this.getHost().equals(m.getTo())){
            return DENIED_UNSPECIFIED;
        }
        return super.receiveMessage(m, from);
    }*/

    @Override
    public void changedConnection(Connection con) {
        super.changedConnection(con);
        if (con.isUp()) {
            DTNHost otherHost = con.getOtherNode(getHost());
            try {
                exchangeKeysWith(otherHost);
                updatePairKeysWith(otherHost);
                updateGroupKeysWith(otherHost);
            } catch (Exception e) {
                //System.err.println("DH key exchange failed with " + otherHost);
            }
        }
    }


    public void updateGroupKeysWith(DTNHost otherHost) throws Exception {
        if (!(otherHost instanceof Broker && this.getHost() instanceof Broker)) {
            return;
        }
        cleanExpiredCaches((Broker) this.getHost(), this.getMsgTtl()*10, SimClock.getTime());
        cleanExpiredCaches((Broker) otherHost, this.getMsgTtl()*10, SimClock.getTime());
        exchangeKeyCaches((Broker) this.getHost(), (Broker) otherHost);
    }

    private void cleanExpiredCaches(Broker broker, double maxAge, double currentTime) {
        List<KeyCache> cachesToCheck = new ArrayList<>(broker.getKeyCaches());
        for (KeyCache keyCache : cachesToCheck) {
            if ((keyCache.getTimeCreated() + maxAge) <= currentTime) {
                broker.deleteKeyCache(keyCache);
            }
        }
    }

    private void exchangeKeyCaches(Broker broker1, Broker broker2) {
        Set<KeyCache> combinedCaches = new HashSet<>();
        combinedCaches.addAll(broker1.getKeyCaches());
        combinedCaches.addAll(broker2.getKeyCaches());
        broker1.getKeyCaches().clear();
        broker1.getKeyCaches().addAll(combinedCaches);
        broker2.getKeyCaches().clear();
        broker2.getKeyCaches().addAll(combinedCaches);
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
        //Add Key to subscriber
        BigInteger secretA = pubSubscriberOrPublisher.modPow(privBroker, primeP); // B^a mod p
        //Add Key to broker
        BigInteger secretB = pubBroker.modPow(privSubscriberOrPublisher, primeP); // A^b mod p
        if (this.getHost() instanceof Broker && otherHost instanceof Subscriber) {
            double totalCreatedSecretKeyWithMsgTtl = ((Subscriber) otherHost).getPairKey().getTimeSecretKeyCreated() + (this.getMsgTtl() * 12);
            if(((Subscriber) otherHost).getPairKey() == null){
                exchangeSharedKey(this.getHost(), otherHost, secretA, secretB);
            }else if (totalCreatedSecretKeyWithMsgTtl <= SimClock.getTime()){
                exchangeSharedKey(this.getHost(), otherHost, secretA, secretB);
            }
        }else if(this.getHost() instanceof Subscriber && otherHost instanceof Broker){
            double totalCreatedSecretKeyWithMsgTtl = ((Subscriber) this.getHost()).getPairKey().getTimeSecretKeyCreated() + (this.getMsgTtl() * 12);
            if(((Subscriber) this.getHost()).getPairKey() == null){
                exchangeSharedKey(otherHost, this.getHost(), secretA, secretB);
            }else if(totalCreatedSecretKeyWithMsgTtl <= SimClock.getTime()){
                exchangeSharedKey(otherHost, this.getHost(), secretA, secretB);
            }
        }else if(this.getHost() instanceof Broker && otherHost instanceof Publisher){
            double totalCreatedSecretKeyWithMsgTtl = ((Publisher) otherHost).getPairKey().getTimeSecretKeyCreated() + (this.getMsgTtl() * 12);
            if(((Publisher) otherHost).getPairKey() == null){
                exchangeSharedKey(otherHost, this.getHost(), secretA, secretB);
            }else if(totalCreatedSecretKeyWithMsgTtl <= SimClock.getTime()){
                exchangeSharedKey(otherHost, this.getHost(), secretA, secretB);
            }
        }else if(this.getHost() instanceof Publisher && otherHost instanceof Broker){
            double totalCreatedSecretKeyWithMsgTtl = ((Publisher) this.getHost()).getPairKey().getTimeSecretKeyCreated() + (this.getMsgTtl() * 12);
            if(((Publisher) this.getHost()).getPairKey() == null){
                exchangeSharedKey(otherHost, this.getHost(), secretA, secretB);
            }else if(totalCreatedSecretKeyWithMsgTtl <= SimClock.getTime()){
                exchangeSharedKey(otherHost, this.getHost(), secretA, secretB);
            }
        }
    }

    private void exchangeSharedKey(DTNHost broker, DTNHost subscriberOrPublisher, BigInteger secretA, BigInteger secretB) throws Exception {
        if(subscriberOrPublisher instanceof Publisher){
            PairKey publisherPairKey = new PairKey();
            publisherPairKey.setSecretKey(convertToAESKey(secretA));
            publisherPairKey.setHostThisKeyBelongsTo(subscriberOrPublisher);
            publisherPairKey.setTimeSecretKeyCreated(SimClock.getTime());
            ((Publisher) subscriberOrPublisher).setPairKey(publisherPairKey);
            for(IKeyListener kl : kListeners){
                kl.keyPairCreated(publisherPairKey);
            }
        }else if(subscriberOrPublisher instanceof Subscriber){
            PairKey subscriberPairKey = new PairKey();
            subscriberPairKey.setSecretKey(convertToAESKey(secretA));
            subscriberPairKey.setHostThisKeyBelongsTo(subscriberOrPublisher);
            subscriberPairKey.setTimeSecretKeyCreated(SimClock.getTime());
            ((Subscriber) subscriberOrPublisher).setPairKey(subscriberPairKey);
            for(IKeyListener kl : kListeners){
                kl.keyPairCreated(subscriberPairKey);
            }
        }
        PairKey brokerPairKey = new PairKey();
        brokerPairKey.setSecretKey(convertToAESKey(secretB));
        brokerPairKey.setHostThisKeyBelongsTo(subscriberOrPublisher);
        brokerPairKey.setTimeSecretKeyCreated(SimClock.getTime());
        ((Broker) broker).addPairKey(brokerPairKey);
        for(IKeyListener kl : kListeners){
            kl.keyPairCreated(brokerPairKey);
        }
    }

    private void updatePairKeysWith(DTNHost otherHost) {
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

    public static SecretKey convertToAESKey(BigInteger bigInt) {
        byte[] keyBytes = bigInt.toByteArray();
        byte[] normalizedKey = new byte[32];
        int length = Math.min(keyBytes.length, 32);
        System.arraycopy(keyBytes, 0, normalizedKey, 32 - length, length);
        return new SecretKeySpec(normalizedKey, "AES");
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
            ((Broker) this.getHost()).processGroup();
            Set<Message> setFilterMsg = new HashSet<>();
            for(Message msgs : this.getHost().getMessageCollection()){
                if(msgs.getProperty(FILTERS) != null){
                    setFilterMsg.add(msgs);
                }
            }
            for (IKeyListener kl : kListeners) {
                kl.generationLoad(setFilterMsg);
            }
        }if(this.getHost() instanceof Subscriber){
            handleSubscriberTransfer(msg);
        }
        return msg;
    }

    private void handleSubscriberTransfer(Message msg) {
        if (!hasEncryptedProperty(msg)) {
            return;
        }
        Object encryptedProperty = msg.getProperty(ENCRYPTED);
        if (!(encryptedProperty instanceof Map)) {
            return;
        }
        processEncryptedMessages((Map<byte[], Set<byte[]>>) encryptedProperty);
    }

    private boolean hasEncryptedProperty(Message msg) {
        return msg.getProperty(ENCRYPTED) != null;
    }

    private void processEncryptedMessages(Map<byte[], Set<byte[]>> encryptedMap) {
        Subscriber subscriber = (Subscriber) this.getHost();
            encryptedMap.forEach((key, values) -> {
                values.forEach(value -> {
                    subscriber.openMessages(key, value);
                });
            });
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

        this.tryAllMessagesToAllConnections();
    }

    @Override
    public GrouperRouter replicate() {
        return new GrouperRouter(this);
    }

}

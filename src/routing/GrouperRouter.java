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

public class GrouperRouter extends ActiveRouter implements PropertySettings {

    private List<IKeyListener> kListeners;
    private static final int DH_KEY_LENGTH = 512;
    private static final int PRIVATE_KEY_LENGTH = 256;
    private static final SecureRandom random = new SecureRandom();
    private BigInteger primeP;
    private BigInteger primeG;

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
        this.primeP = r.primeP;
        this.primeG = r.primeG;
        this.kListeners = r.kListeners;
    }

    @Override
    public void init(DTNHost host, List<MessageListener> mListeners, List<IKeyListener> kListeners) {
        super.init(host, mListeners, kListeners);
        this.kListeners = kListeners;
    }

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

    private void initializeDHParameters() {
        if (primeP == null || primeG == null) {
            primeP = BigInteger.probablePrime(DH_KEY_LENGTH, random);
            primeG = BigInteger.probablePrime(DH_KEY_LENGTH, random);
        }
    }

    private void exchangeKeysWith(DTNHost otherHost) throws Exception {
        initializeDHParameters();

        BigInteger privThis = new BigInteger(PRIVATE_KEY_LENGTH, random);
        BigInteger privOther = new BigInteger(PRIVATE_KEY_LENGTH, random);

        BigInteger pubThis = primeG.modPow(privThis, primeP);
        BigInteger pubOther = primeG.modPow(privOther, primeP);

        BigInteger secretThis = pubOther.modPow(privThis, primeP);
        BigInteger secretOther = pubThis.modPow(privOther, primeP);

        if (shouldExchangeKeys(this.getHost(), otherHost)) {
            exchangeSharedKey(determineBroker(this.getHost(), otherHost),
                    determineNonBroker(this.getHost(), otherHost),
                    secretThis, secretOther);
        }
    }

    private boolean shouldExchangeKeys(DTNHost host1, DTNHost host2) {
        PairKey pairKey = getPairKeyForHost(isBroker(host1) ? host2 : host1);
        if (pairKey == null) return true;

        double expirationTime = pairKey.getTimeSecretKeyCreated() + this.getMsgTtl();
        return expirationTime <= SimClock.getTime();
    }

    private DTNHost determineBroker(DTNHost host1, DTNHost host2) {
        return isBroker(host1) ? host1 : host2;
    }

    private DTNHost determineNonBroker(DTNHost host1, DTNHost host2) {
        return isBroker(host1) ? host2 : host1;
    }

    private boolean isBroker(DTNHost host) {
        return host instanceof Broker;
    }

    private PairKey getPairKeyForHost(DTNHost host) {
        if (host instanceof Subscriber) {
            return ((Subscriber) host).getPairKey();
        } else if (host instanceof Publisher) {
            return ((Publisher) host).getPairKey();
        }
        return null;
    }

    private void exchangeSharedKey(DTNHost broker, DTNHost nonBroker,
                                   BigInteger secretA, BigInteger secretB) throws Exception {
        PairKey nonBrokerKey = createPairKey(nonBroker, secretA);
        setPairKey(nonBroker, nonBrokerKey);

        PairKey brokerKey = createPairKey(nonBroker, secretB);
        ((Broker) broker).addPairKey(brokerKey);

        notifyKeyListeners(nonBrokerKey, brokerKey);
    }

    private PairKey createPairKey(DTNHost host, BigInteger secret) throws NoSuchAlgorithmException {
        PairKey key = new PairKey();
        key.setSecretKey(convertToAESKey(secret));
        key.setHostThisKeyBelongsTo(host);
        key.setTimeSecretKeyCreated(SimClock.getTime());
        return key;
    }

    private void setPairKey(DTNHost host, PairKey key) {
        if (host instanceof Subscriber) {
            ((Subscriber) host).setPairKey(key);
        } else if (host instanceof Publisher) {
            ((Publisher) host).setPairKey(key);
        }
    }

    private void notifyKeyListeners(PairKey... keys) {
        for (PairKey key : keys) {
            for (IKeyListener listener : kListeners) {
                listener.keyPairCreated(key);
            }
        }
    }

    private void updatePairKeysWith(DTNHost otherHost) throws Exception {
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

    public void updateGroupKeysWith(DTNHost otherHost) throws Exception {
        if (!(otherHost instanceof Broker && this.getHost() instanceof Broker)) {
            return;
        }

        cleanExpiredCaches((Broker) this.getHost(), this.getMsgTtl() * 1000, SimClock.getTime());
        cleanExpiredCaches((Broker) otherHost, this.getMsgTtl() * 1000, SimClock.getTime());

        exchangeKeyCaches((Broker) this.getHost(), (Broker) otherHost);
    }

    private void cleanExpiredCaches(Broker broker, double maxAge, double currentTime) {
        List<KeyCache> cachesToCheck = new ArrayList<>(broker.getKeyCaches());

        for (KeyCache keyCache : cachesToCheck) {
            if ((keyCache.getTimeCreated() + maxAge) <= currentTime) {
                System.out.println("clean expired cache key");
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

    public static SecretKey convertToAESKey(BigInteger bigInt) throws NoSuchAlgorithmException {
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
            handleBrokerTransfer();
        } else if (this.getHost() instanceof Subscriber) {
            handleSubscriberTransfer(msg);
        }

        return msg;
    }

    private void handleBrokerTransfer() {
        ((Broker) this.getHost()).makeGroups();
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
                if (subscriber.openMessages(key, value)) {
                    System.out.println("success");
                }
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

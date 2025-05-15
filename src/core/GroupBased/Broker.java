package core.GroupBased;

import GroupBased.*;
import GroupBased.Model.*;
import core.*;
import movement.MovementModel;
import routing.MessageRouter;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static GroupBased.LoremIpsumGenerator.generateLoremIpsum;

public class Broker extends DTNHost implements PropertySettings{
    private List<MergedInterval> groups;
    private final List<PairKey> pairKeys;
    private Map<byte[], Set<byte[]>> encryptedEventsGrouped; //saving encrypted event that encrypted with List of encrypted KG\
    private final List<IKeyListener> keyListeners;
    private Set<KeyCache> keyCaches;
    private Set<RawGroup> rawGroups;
    /**
     * Creates a new DTNHost.
     *
     * @param msgLs        Message listeners
     * @param movLs        Movement listeners
     * @param groupId      GroupID of this host
     * @param interf       List of NetworkInterfaces for the class
     * @param comBus       Module communication bus object
     * @param mmProto      Prototype of the movement model of this host
     * @param mRouterProto Prototype of the message router of this host
     */
    public Broker(List<MessageListener> msgLs, List<MovementListener> movLs, String groupId, List<NetworkInterface> interf, ModuleCommunicationBus comBus, MovementModel mmProto, MessageRouter mRouterProto, List<IKeyListener> keyLs) {
        super(msgLs, movLs, groupId, interf, comBus, mmProto, mRouterProto, keyLs);
        this.pairKeys = new ArrayList<>();
        this.encryptedEventsGrouped = new HashMap<>();
        this.keyListeners = keyLs;
        this.keyCaches = new HashSet<>();
        this.rawGroups = new HashSet<>();
    }

    public void makeGroups(){
        for(Message msg : this.getMessageCollection()){
            if(msg.getProperty(FILTERS) != null && msg.getProperty(FILTERS) instanceof Set<?>){
                Set<FilterData> filters = (Set<FilterData>) msg.getProperty(FILTERS);
                RawGroup rwGroup = new RawGroup();
                rwGroup.setSubscriber(msg.getFrom());
                rwGroup.setEvents(filters);
                rawGroups.add(rwGroup);
            }
        }

        groups = groupedFilters();

        if(groups == null){
            return;
        }
        notifyGroupGeneration(groups);
    }

    private List<MergedInterval> groupedFilters(){
        Map<DTNHost, List<int[]>> subscribers = new HashMap<>();
        for(RawGroup rwGroup : this.rawGroups){
            for(FilterData fd : rwGroup.getEvents()){
                subscribers.put(rwGroup.getSubscriber(), List.of(new int[]{fd.getStart(), fd.getEnd()}));
            }
        }

        TreeSet<Integer> boundaries = new TreeSet<>();
        for (var intervals : subscribers.values()) {
            for (int[] interval : intervals) {
                boundaries.add(interval[0]);
                boundaries.add(interval[1]);
            }
        }

        List<Segment> result = new ArrayList<>();
        List<Integer> boundaryList = new ArrayList<>(boundaries);

        for (int i = 0; i < boundaryList.size() - 1; i++) {
            int start = boundaryList.get(i);
            int end = boundaryList.get(i + 1);
            Set<DTNHost> activeSubs = new HashSet<>();
            for (Map.Entry<DTNHost, List<int[]>> entry : subscribers.entrySet()) {
                DTNHost sub = entry.getKey();
                for (int[] interval : entry.getValue()) {
                    if (interval[0] < end && interval[1] > start) {
                        activeSubs.add(sub);
                    }
                }
            }

            if (!activeSubs.isEmpty()) {
                result.add(new Segment(start, end, new HashSet<>(activeSubs)));
            }
        }

        List<MergedInterval> groupDone = new ArrayList<>();
        for (Segment s : result) {
            MergedInterval mgl = new MergedInterval(s.start, s.end, s.subscribers);
            groupDone.add(mgl);
        }

        if(!groupDone.isEmpty()){
            return groupDone;
        }

        return null;
    }

    static class Segment {
        int start, end;
        Set<DTNHost> subscribers;

        Segment(int start, int end, Set<DTNHost> subscribers) {
            this.start = start;
            this.end = end;
            this.subscribers = subscribers;
        }
    }

    public void processGroup(){
        Set<EventData> events = extractEventsFromMessages();
        if (groups == null || events.isEmpty()) return;
        for (MergedInterval group : groups) {
            if (hasMatchingEvents(events, group)) {
                handleGroupEncryption(group);
            }
        }
    }

    private Set<EventData> extractEventsFromMessages() {
        Set<EventData> events = new HashSet<>();
        for (Message msg : getMessageCollection()) {
            if (msg.getProperty(EVENTS) != null) {
                Set<EventData> msgEvents = (Set<EventData>) msg.getProperty(EVENTS);
                events.addAll(msgEvents);
            }
        }
        return events;
    }

    private boolean hasMatchingEvents(Set<EventData> events, MergedInterval group) {
        return events.stream()
                .anyMatch(e -> e.getNum() >= group.getStart() && e.getNum() <= group.getEnd());
    }

    private void handleGroupEncryption(MergedInterval group) {
        Optional<SecretKey> existingKey = findCachedKey(group.getSenders());

        if (existingKey.isEmpty()) {
            generateAndCacheKey(group);
        }
    }

    private Optional<SecretKey> findCachedKey(Set<DTNHost> senders) {
        return keyCaches.stream()
                .filter(entry -> entry.getSenders().equals(senders))
                .map(KeyCache::getSecretKey)
                .findFirst();
    }

    private void generateAndCacheKey(MergedInterval group) {
        try {
            SecretKey newKey = generateAESKey(256);
            KeyCache kc = new KeyCache();
            kc.setSenders(group.getSenders());
            kc.setSecretKey(newKey);
            kc.setTimeCreated(SimClock.getTime());
            notifyGroupKeyGenerationForLatencies(SimClock.getTime());
            keyCaches.add(kc);
            notifyKeyGeneration(newKey);
            this.setEncryptedEventsGrouped(encryptEventGroups());
        } catch (Exception e) {
            System.err.println("Key generation failed: " + e.getMessage());
        }
    }

    private void notifyKeyGeneration(SecretKey key) {
        if (keyListeners != null) {
            keyListeners.forEach(kl ->
                    kl.groupKeyGeneratedByBroker(key, this)
            );
        }
    }

    private void notifyGroupGeneration(List<MergedInterval> group) {
        if (keyListeners != null) {
            keyListeners.forEach(kl ->
                    kl.generatedGroups(this, group)
            );
        }
    }

    private void notifyGroupKeyGenerationForLatencies(double load){
        if (keyListeners != null) {
            keyListeners.forEach(kl ->
                    kl.latenciesGroupKey(load)
            );
        }
    }

    public Map<byte[], Set<byte[]>> encryptEventGroups() {
        Map<byte[], Set<byte[]>> result = new HashMap<>();
        for (MergedInterval group : groups) {
            SecretKey groupKey = findMatchingKey(keyCaches, group.getSenders());
            if (groupKey == null) continue;
            try {
                byte[] encryptedEvent = encryptEvent(generateLoremIpsum(5), groupKey);
                Set<byte[]> encryptedKeys = new HashSet<>();
                for (DTNHost subscriber : group.getSenders()) {
                    for (PairKey pairKey : pairKeys) {
                        if (pairKey.getHostThisKeyBelongsTo().equals(subscriber)) {
                            encryptedKeys.add(encryptKey(groupKey, pairKey.getSecretKey()));
                            break;
                        }
                    }
                }

                if (!encryptedKeys.isEmpty()) {
                    result.put(encryptedEvent, encryptedKeys);
                }
            } catch (Exception e) {
                System.err.println("Encryption failed for group: " + e.getMessage());
            }
        }

        return result;
    }

    public SecretKey findMatchingKey(Set<KeyCache> cachedEvents, Set<DTNHost> targetHosts) {
        for (KeyCache entry : cachedEvents) {
            Set<DTNHost> currentHosts = entry.getSenders();
            if (currentHosts.equals(targetHosts)) {
                return entry.getSecretKey();
            }
        }
        return null; // not found
    }

    public byte[] encryptEvent(String data, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    public Set<KeyCache> getKeyCaches() {
        return keyCaches;
    }

    public boolean deleteKeyCache(KeyCache keyCache) {
        if (keyCaches == null || keyCaches.isEmpty()) {
            return false;
        }
        return keyCaches.remove(keyCache);
    }

    public static SecretKey generateAESKey(int keySize) throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(keySize);
        return keyGen.generateKey();
    }

    public byte[] encryptKey(SecretKey keyToEncrypt, SecretKey encryptionKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey);
        return cipher.doFinal(keyToEncrypt.getEncoded());
    }

    public void setEncryptedEventsGrouped(Map<byte[], Set<byte[]>> encryptedEventsGrouped) {
        this.encryptedEventsGrouped = encryptedEventsGrouped;
    }

    public Map<byte[], Set<byte[]>> getEncryptedEventsGrouped() {
        return encryptedEventsGrouped;
    }

    public List<MergedInterval> getGroups() {
        return groups;
    }

    public List<PairKey> getPairKeys() {
        return pairKeys;
    }

    public void addPairKey(PairKey pairKey){
        this.pairKeys.add(pairKey);
    }

    /**
     * Creates a new message to this host's router
     * @param m The message to create
     */
    public void createNewMessage(Message m) {
        if(m.getProperty(ENCRYPTED) != null){
            if(this.keyListeners != null) {
                for(IKeyListener kl : this.keyListeners) {
                    kl.encryptedEventsCreated(m);
                }
            }
        }
        this.getRouter().createNewMessage(m);
    }
}

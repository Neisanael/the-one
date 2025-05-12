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
    private final Map<byte[], List<byte[]>> encryptedEventsGrouped; //saving encrypted event that encrypted with List of encrypted KG\
    private final List<IKeyListener> keyListeners;
    private Set<KeyCache> keyCaches;
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
        this.groups = new ArrayList<>();
        this.keyListeners = keyLs;
        this.keyCaches = new HashSet<>();
    }

    public void makeGroups(){
        groups = mergedSubscriberFilters();

        if (keyListeners != null) {
            groups.forEach(group ->
                    keyListeners.forEach(kl ->
                            kl.generatedGroups(this, group)
                    )
            );
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
            if (msg.getProperty(EVENTS) instanceof Set<?> eventSet) {
                try {
                    events.addAll((Set<EventData>)eventSet);
                } catch (ClassCastException e) {
                    System.err.println("Event type mismatch in message");
                }
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
            keyCaches.add(kc);
            notifyKeyGeneration(newKey);
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

    public Map<byte[], List<byte[]>> getEncryptedEventsGrouped() {
        return encryptedEventsGrouped;
    }

    public List<MergedInterval> getGroups() {
        return groups;
    }

    private List<MergedInterval> mergedSubscriberFilters() {
        List<FilterDataWithSender> allFilters = new ArrayList<>();
        for (Message msg : getMessageCollection()) {
            if (msg.getProperty(FILTERS) != null && msg.getProperty(FILTERS) instanceof Set<?>) {
                try {
                    Set<FilterData> restoredSet = (Set<FilterData>) msg.getProperty(FILTERS);
                    for (FilterData fd : restoredSet) {
                        allFilters.add(new FilterDataWithSender(fd, msg.getFrom()));
                    }
                } catch (ClassCastException e) {
                    System.err.println("Failed to cast - wrong generic type");
                }
            }
        }

        if (allFilters.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Boolean, List<FilterDataWithSender>> groupedByIssue = allFilters.stream()
                .collect(Collectors.groupingBy(fws -> fws.getFilterData().getIssue()));
        List<MergedInterval> combinedResults = new ArrayList<>();
        for (Map.Entry<Boolean, List<FilterDataWithSender>> entry : groupedByIssue.entrySet()) {
            List<FilterDataWithSender> filters = entry.getValue();
            filters.sort(Comparator.comparingInt(fws -> fws.getFilterData().getStart()));
            List<MergedInterval> mergedIntervals = mergeIntervals(filters);
            combinedResults.addAll(mergedIntervals);
        }
        combinedResults.sort(Comparator.comparingInt(MergedInterval::getStart));
        return combinedResults;
    }

    private List<MergedInterval> mergeIntervals(List<FilterDataWithSender> filtersWithSenders) {
        if (filtersWithSenders.isEmpty()) {
            return Collections.emptyList();
        }
        List<IntervalPoint> points = new ArrayList<>();
        for (FilterDataWithSender fws : filtersWithSenders) {
            points.add(new IntervalPoint(fws.getFilterData().getStart(), true, fws.getSender()));
            points.add(new IntervalPoint(fws.getFilterData().getEnd(), false, fws.getSender()));
        }
        Collections.sort(points);

        List<MergedInterval> result = new ArrayList<>();
        Set<DTNHost> activeSenders = new HashSet<>();
        int prevPoint = -1;

        for (IntervalPoint point : points) {
            if (!activeSenders.isEmpty() && prevPoint != point.getValue()) {
                result.add(new MergedInterval(prevPoint, point.getValue(), new HashSet<>(activeSenders)));
            }

            if (point.isStart()) {
                activeSenders.add(point.getSender());
            } else {
                activeSenders.remove(point.getSender());
            }

            prevPoint = point.getValue();
        }

        return result;
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

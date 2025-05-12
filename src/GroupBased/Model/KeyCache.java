package GroupBased.Model;

import core.DTNHost;

import javax.crypto.SecretKey;
import java.util.HashSet;
import java.util.Set;

public class KeyCache {
    private double timeCreated;
    private SecretKey secretKey;
    private Set<DTNHost> senders;

    public KeyCache() {
        this.senders = new HashSet<>();
    }

    public double getTimeCreated() {
        return timeCreated;
    }

    public void setTimeCreated(double timeCreated) {
        this.timeCreated = timeCreated;
    }

    public SecretKey getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(SecretKey secretKey) {
        this.secretKey = secretKey;
    }

    public Set<DTNHost> getSenders() {
        return senders;
    }

    public void setSenders(Set<DTNHost> senders) {
        this.senders = senders;
    }

    public void addSender(DTNHost sender) {
        this.senders.add(sender);
    }
}

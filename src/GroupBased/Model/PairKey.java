package GroupBased.Model;

import core.DTNHost;

import javax.crypto.SecretKey;

public class PairKey {

    private SecretKey secretKey;
    private double timeSecretKeyCreated;
    private DTNHost hostThisKeyBelongsTo;

    public SecretKey getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(SecretKey secretKey) {
        this.secretKey = secretKey;
    }

    public double getTimeSecretKeyCreated() {
        return timeSecretKeyCreated;
    }

    public void setTimeSecretKeyCreated(double timeSecretKeyCreated) {
        this.timeSecretKeyCreated = timeSecretKeyCreated;
    }

    public DTNHost getHostThisKeyBelongsTo() {
        return hostThisKeyBelongsTo;
    }

    public void setHostThisKeyBelongsTo(DTNHost hostThisKeyBelongsTo) {
        this.hostThisKeyBelongsTo = hostThisKeyBelongsTo;
    }
}

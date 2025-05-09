package core.GroupBased;

import core.*;
import movement.MovementModel;
import routing.MessageRouter;

import javax.crypto.SecretKey;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Publisher extends DTNHost {
    //private final Map<DTNHost, SecretKey> publicSecretKey;

    private PairKey pairKey;
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
    public Publisher(List<MessageListener> msgLs, List<MovementListener> movLs, String groupId, List<NetworkInterface> interf, ModuleCommunicationBus comBus, MovementModel mmProto, MessageRouter mRouterProto) {
        super(msgLs, movLs, groupId, interf, comBus, mmProto, mRouterProto);
        //publicSecretKey = new HashMap<>();
        pairKey = new PairKey();
    }

    /*public Map<DTNHost, SecretKey> getPublicSecretKey() {
        return publicSecretKey;
    }

    public void addPublicSecretKey(DTNHost host, SecretKey publicSecretKey) {
        this.publicSecretKey.put(host, publicSecretKey);
    }*/

    public PairKey getPairKey() {
        return pairKey;
    }

    public void setPairKey(PairKey pairKey) {
        this.pairKey = pairKey;
    }
}

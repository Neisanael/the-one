package core.GroupBased;

import core.*;
import movement.MovementModel;
import routing.MessageRouter;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Subscriber extends DTNHost {
    private Map<DTNHost, BigInteger> publicSecretKey;

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
    public Subscriber(List<MessageListener> msgLs, List<MovementListener> movLs, String groupId, List<NetworkInterface> interf, ModuleCommunicationBus comBus, MovementModel mmProto, MessageRouter mRouterProto) {
        super(msgLs, movLs, groupId, interf, comBus, mmProto, mRouterProto);
        Random rand = new Random();
        publicSecretKey = new HashMap<>();
    }

    public Map<DTNHost, BigInteger> getPublicSecretKey() {
        return publicSecretKey;
    }

    public void setPublicSecretKey(Map<DTNHost, BigInteger> publicSecretKey) {
        this.publicSecretKey = publicSecretKey;
    }

    public void addPublicSecretKey(DTNHost host, BigInteger publicSecretKey) {
        this.publicSecretKey.put(host, publicSecretKey);
    }

    public String openMessages() {
        return "";
    }
}

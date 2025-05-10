package core.GroupBased;

import GroupBased.PropertySettings;
import core.*;
import movement.MovementModel;
import routing.MessageRouter;

import javax.crypto.SecretKey;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Publisher extends DTNHost implements PropertySettings {
    private PairKey pairKey;
    private List<IKeyListener> keyListeners;
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
    public Publisher(List<MessageListener> msgLs, List<MovementListener> movLs, String groupId, List<NetworkInterface> interf, ModuleCommunicationBus comBus, MovementModel mmProto, MessageRouter mRouterProto, List<IKeyListener> keyLs) {
        super(msgLs, movLs, groupId, interf, comBus, mmProto, mRouterProto, keyLs);
        this.pairKey = new PairKey();
        this.keyListeners = keyLs;
    }

    public PairKey getPairKey() {
        return pairKey;
    }

    public void setPairKey(PairKey pairKey) {
        this.pairKey = pairKey;
    }

    /**
     * Creates a new message to this host's router
     * @param m The message to create
     */
    public void createNewMessage(Message m) {
        if(m.getProperty(EVENTS) != null){
            if(this.keyListeners != null) {
                for(IKeyListener kl : this.keyListeners) {
                    kl.eventsCreated(m);
                }
            }
        }
        this.getRouter().createNewMessage(m);
    }
}

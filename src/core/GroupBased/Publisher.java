package core.GroupBased;

import core.*;
import movement.MovementModel;
import routing.MessageRouter;

import java.util.List;

public class Publisher extends DTNHost {
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
    }
}

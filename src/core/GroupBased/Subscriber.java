package core.GroupBased;

import GroupBased.PropertySettings;
import core.*;
import movement.MovementModel;
import routing.MessageRouter;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Subscriber extends DTNHost implements PropertySettings {
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
    public Subscriber(List<MessageListener> msgLs, List<MovementListener> movLs, String groupId, List<NetworkInterface> interf, ModuleCommunicationBus comBus, MovementModel mmProto, MessageRouter mRouterProto, List<IKeyListener> keyLs) {
        super(msgLs, movLs, groupId, interf, comBus, mmProto, mRouterProto, keyLs);
        this.pairKey = new PairKey();
        this.keyListeners = keyLs;
    }

    public Boolean openMessages(byte[] payload,byte[] val) {
        try {
            String value = decryptEvent(payload, decryptSecretKey(val, getPairKey().getSecretKey()));
            if(this.keyListeners != null) {
                for(IKeyListener kl : this.keyListeners) {
                    kl.openedMessage(getPairKey().getSecretKey(), this);
                }
            }
            System.out.println(value);
            return true;
        } catch (Exception e) {
            //System.err.println("error decrypting: " + e.getMessage());
            // Decryption failed with this key, try the next one
        }
        return false;
    }

    private String decryptEvent(byte[] encryptedData, SecretKey key) throws Exception {
        byte[] decryptedBytes = decryptAES(encryptedData, key);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    private static byte[] decryptAES(byte[] encryptedData, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(encryptedData);
    }

    public SecretKey decryptSecretKey(byte[] encryptedKey, SecretKey decryptionKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, decryptionKey);
        byte[] decryptedKeyBytes = cipher.doFinal(encryptedKey);

        // Reconstruct the SecretKey from the decrypted bytes
        return new SecretKeySpec(decryptedKeyBytes, "AES");
    }

    public PairKey getPairKey() {
        return pairKey;
    }

    public void setPairKey(PairKey pairKey) {
        this.pairKey = pairKey;
    }
}

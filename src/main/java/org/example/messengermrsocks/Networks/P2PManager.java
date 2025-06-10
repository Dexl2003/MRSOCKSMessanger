package org.example.messengermrsocks.Networks;

import org.example.messengermrsocks.model.Peoples.User;
import org.example.messengermrsocks.model.Peoples.Contact;

public class P2PManager {
    private static final int P2P_PORT = 9000;
    private final P2PConnectionManager p2pConnectionManager;
    private final P2PInviteManager p2pInviteManager;
    private final User currentUser;

    public P2PManager(User currentUser) {
        this.currentUser = currentUser;
        this.p2pConnectionManager = new P2PConnectionManager();
        this.p2pInviteManager = new P2PInviteManager(currentUser, new P2PInviteManager.P2PInviteListener() {
            @Override
            public void onInviteAccepted(String fromUsername, String fromIp, int fromPort) {
                // Handle accepted invite - create or update contact
                Contact contact = new Contact(fromUsername, 
                    java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
                    "/images/image.png");
                contact.setIp(fromIp);
                contact.setMainPort(P2P_PORT); // Use fixed port
                p2pConnectionManager.addContact(contact);
            }

            @Override
            public void onInviteRejected(String fromUsername) {
                // Handle rejected invite - maybe show notification
                System.out.println("P2P invite from " + fromUsername + " was rejected");
            }
        });
    }

    public P2PConnectionManager getP2pConnectionManager() {
        return p2pConnectionManager;
    }

    public boolean requestP2PChannel(Contact contact) {
        if (contact == null || contact.getIp() == null) {
            return false;
        }
        // Always use the fixed P2P port
        return p2pInviteManager.sendInvite(contact.getName(), contact.getIp(), P2P_PORT);
    }

    public void shutdown() {
        p2pConnectionManager.shutdown();
        p2pInviteManager.shutdown();
    }
} 
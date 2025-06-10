package org.example.messengermrsocks.Networks;

import org.example.messengermrsocks.model.Peoples.User;
import org.example.messengermrsocks.model.Peoples.Contact;

public class P2PManager {
    private static final int P2P_PORT = 9000;
    private final P2PConnectionManager p2pConnectionManager;
    private final P2PInviteManager p2pInviteManager;
    private final User currentUser;
    private ReconnectListener reconnectListener;

    public interface ReconnectListener {
        void onReconnectRequest(String fromUsername, String fromIp, int fromPort);
    }

    public void setReconnectListener(ReconnectListener listener) {
        this.reconnectListener = listener;
    }

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
                contact.setMainPort(P2P_PORT);
                p2pConnectionManager.addContact(contact);
            }

            @Override
            public void onInviteRejected(String fromUsername) {
                System.out.println("P2P invite from " + fromUsername + " was rejected");
            }

            @Override
            public void onReconnectRequest(String fromUsername, String fromIp, int fromPort) {
                System.out.println("Received reconnect request from " + fromUsername);
                Contact contact = new Contact(fromUsername,
                    java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
                    "/images/image.png");
                contact.setIp(fromIp);
                contact.setMainPort(P2P_PORT);
                p2pConnectionManager.addContact(contact);
                
                // Уведомляем слушателя о повторном подключении
                if (reconnectListener != null) {
                    reconnectListener.onReconnectRequest(fromUsername, fromIp, fromPort);
                }
            }
        });
    }

    public P2PConnectionManager getP2pConnectionManager() {
        return p2pConnectionManager;
    }

    public P2PInviteManager getP2pInviteManager() {
        return p2pInviteManager;
    }

    public boolean requestP2PChannel(Contact contact) {
        if (contact == null || contact.getIp() == null) {
            return false;
        }
        // Always use the fixed P2P port
        return p2pInviteManager.sendInvite(contact.getName(), contact.getIp(), P2P_PORT);
    }

    public void shutdown() {
        System.out.println("P2PManager: Starting shutdown process...");
        try {
            // Закрываем все активные P2P соединения
            if (p2pConnectionManager != null) {
                System.out.println("P2PManager: Shutting down connection manager...");
                p2pConnectionManager.shutdown();
            }
            // Закрываем менеджер инвайтов
            if (p2pInviteManager != null) {
                System.out.println("P2PManager: Shutting down invite manager...");
                p2pInviteManager.shutdown();
            }
            System.out.println("P2PManager: Shutdown completed successfully");
        } catch (Exception e) {
            System.err.println("P2PManager: Error during shutdown: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 
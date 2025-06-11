package org.example.messengermrsocks.Networks;

import org.example.messengermrsocks.model.Peoples.User;
import org.example.messengermrsocks.model.Peoples.Contact;
import org.example.messengermrsocks.Networks.P2PInviteManager.P2PInviteListener;

import java.util.List;

public class P2PManager {
    private static final int P2P_PORT = 9000;
    private final P2PConnectionManager p2pConnectionManager;
    private final P2PInviteManager p2pInviteManager;
    private final User currentUser;
    private ReconnectListener reconnectListener;
    private P2PInviteListener uiListener;

    public interface ReconnectListener {
        void onReconnectRequest(String fromUsername, String fromIp, int fromPort);
    }

    public void setReconnectListener(ReconnectListener listener) {
        this.reconnectListener = listener;
    }

    public void setUIListener(P2PInviteListener listener) {
        this.uiListener = listener;
    }

    public P2PManager(User currentUser) {
        this.currentUser = currentUser;
        this.p2pInviteManager = new P2PInviteManager(currentUser, null);
        this.p2pConnectionManager = new P2PConnectionManager(p2pInviteManager);
        
        this.p2pInviteManager.setInviteListener(new P2PInviteManager.P2PInviteListener() {
            @Override
            public void onInviteAccepted(String fromUsername, String fromIp, int fromPort) {
                System.out.println("[P2PManager] onInviteAccepted вызван для " + fromUsername + " (IP: " + fromIp + ", Port: " + fromPort + ")");
                // Handle accepted invite - create or update contact
                Contact contact = new Contact(fromUsername, 
                    java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
                    "/images/image.png");
                contact.setIp(fromIp);
                contact.setMainPort(P2P_PORT);
                System.out.println("[P2PManager] Создан/обновлен контакт: " + contact.getName() + " (IP: " + contact.getIp() + ")");
                p2pConnectionManager.addContact(contact);
                
                // Получаем порты, которые мы отправили собеседнику в инвайте
                List<Integer> myPorts = p2pInviteManager.getMyPortsForContact(fromUsername);
                System.out.println("[P2PManager] Получены наши порты для " + fromUsername + ": " + myPorts);
                if (myPorts == null || myPorts.isEmpty()) {
                    System.err.println("[P2PManager] Не удалось получить порты для " + fromUsername);
                    return;
                }

                // Проверяем, не запущены ли уже слушатели на этих портах
                for (Integer port : myPorts) {
                    if (p2pConnectionManager.isPortActive(port)) {
                        System.out.println("[P2PManager] Слушатель для порта " + port + " уже активен");
                        return;
                    }
                }
                
                System.out.println("[P2PManager] Запуск слушателей на НАШИХ портах (которые мы отправили собеседнику): " + myPorts);
                try {
                    p2pConnectionManager.startListenersForPorts(myPorts);
                    System.out.println("[P2PManager] Вызов startListenersForPorts завершен");
                } catch (Exception e) {
                    System.err.println("[P2PManager] Ошибка при запуске слушателей: " + e.getMessage());
                    e.printStackTrace();
                }

                // Уведомляем UI listener
                if (uiListener != null) {
                    uiListener.onInviteAccepted(fromUsername, fromIp, fromPort);
                }
            }

            @Override
            public void onInviteRejected(String fromUsername) {
                System.out.println("[P2PManager] P2P invite from " + fromUsername + " was rejected");
                // Уведомляем UI listener
                if (uiListener != null) {
                    uiListener.onInviteRejected(fromUsername);
                }
            }

            @Override
            public void onReconnectRequest(String fromUsername, String fromIp, int fromPort) {
                System.out.println("[P2PManager] onReconnectRequest вызван для " + fromUsername + " (IP: " + fromIp + ", Port: " + fromPort + ")");
                Contact contact = new Contact(fromUsername,
                    java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
                    "/images/image.png");
                contact.setIp(fromIp);
                contact.setMainPort(P2P_PORT);
                System.out.println("[P2PManager] Создан/обновлен контакт для реконнекта: " + contact.getName() + " (IP: " + contact.getIp() + ")");
                p2pConnectionManager.addContact(contact);
                
                // Получаем порты, которые мы отправили собеседнику в инвайте
                List<Integer> myPorts = p2pInviteManager.getMyPortsForContact(fromUsername);
                System.out.println("[P2PManager] Получены наши порты для реконнекта " + fromUsername + ": " + myPorts);
                if (myPorts == null || myPorts.isEmpty()) {
                    System.err.println("[P2PManager] Не удалось получить порты для реконнекта " + fromUsername);
                    return;
                }

                // Проверяем, не запущены ли уже слушатели на этих портах
                for (Integer port : myPorts) {
                    if (p2pConnectionManager.isPortActive(port)) {
                        System.out.println("[P2PManager] Слушатель для порта " + port + " уже активен");
                        return;
                    }
                }
                
                System.out.println("[P2PManager] Запуск слушателей на НАШИХ портах для реконнекта (которые мы отправили собеседнику): " + myPorts);
                try {
                    p2pConnectionManager.startListenersForPorts(myPorts);
                    System.out.println("[P2PManager] Вызов startListenersForPorts для реконнекта завершен");
                } catch (Exception e) {
                    System.err.println("[P2PManager] Ошибка при запуске слушателей для реконнекта: " + e.getMessage());
                    e.printStackTrace();
                }
                
                // Уведомляем слушателя о повторном подключении
                if (reconnectListener != null) {
                    reconnectListener.onReconnectRequest(fromUsername, fromIp, fromPort);
                }
                // Уведомляем UI listener
                if (uiListener != null) {
                    uiListener.onReconnectRequest(fromUsername, fromIp, fromPort);
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
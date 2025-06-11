package org.example.messengermrsocks.Networks;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.example.messengermrsocks.model.Messages.P2PInvite;
import org.example.messengermrsocks.model.Messages.P2PInviteResponse;
import org.example.messengermrsocks.model.Peoples.User;
import org.example.messengermrsocks.Networks.P2PCryptoUtil;
import org.example.messengermrsocks.Networks.P2PSession;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.SecretKey;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

public class P2PInviteManager {
    private static final int INVITE_PORT = 9000;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final User currentUser;
    private P2PInviteListener inviteListener;
    private volatile boolean isRunning = true;
    private final Map<String, Long> lastInviteTime = new ConcurrentHashMap<>();
    private static final long INVITE_COOLDOWN = 5000;
    private static final long RECONNECT_COOLDOWN = 5000; // 5 seconds

    // Сессии по имени пользователя
    private final Map<String, P2PSession> sessionMap = new ConcurrentHashMap<>();
    private final Map<String, List<Integer>> remotePortsMap = new ConcurrentHashMap<>();
    private final Map<String, List<Integer>> myPortsMap = new ConcurrentHashMap<>();  // Добавляем мапу для хранения наших портов

    public interface P2PInviteListener {
        void onInviteAccepted(String fromUsername, String fromIp, int fromPort);
        void onInviteRejected(String fromUsername);
        void onReconnectRequest(String fromUsername, String fromIp, int fromPort);
    }

    public P2PInviteManager(User currentUser, P2PInviteListener listener) {
        this.currentUser = currentUser;
        this.inviteListener = listener;
        startInviteListener();
        System.out.println("P2PInviteManager started, listening on port " + INVITE_PORT);
    }

    private void startInviteListener() {
        executorService.submit(() -> {
            try (ServerSocket serverSocket = new ServerSocket(INVITE_PORT)) {
                System.out.println("Started listening for P2P invites on port " + INVITE_PORT);
                while (isRunning) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        System.out.println("Received connection from " + clientSocket.getInetAddress());
                        handleIncomingConnection(clientSocket);
                    } catch (IOException e) {
                        if (isRunning) {
                            System.err.println("Error accepting connection: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("Failed to start invite listener: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void handleIncomingConnection(Socket socket) {
        executorService.submit(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                 PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {
                
                String jsonInvite = reader.readLine();
                if (jsonInvite == null) {
                    System.err.println("No invite received");
                    return;
                }
                System.out.println("Received invite: " + jsonInvite);
                P2PInvite invite = objectMapper.readValue(jsonInvite, P2PInvite.class);
                System.out.println("handleIncomingConnection: удаленный IP (откуда пришел запрос) = " + socket.getInetAddress().getHostAddress());
                
                // Генерируем ECDH-ключи для этой сессии
                KeyPair ecdhKeyPair = P2PCryptoUtil.generateECDHKeyPair();
                String myPubKeyBase64 = Base64.getEncoder().encodeToString(ecdhKeyPair.getPublic().getEncoded());
                
                // Получаем публичный ключ собеседника из инвайта
                String remotePubKeyBase64 = invite.getEcdhPublicKeyBase64();
                PublicKey remotePubKey = null;
                if (remotePubKeyBase64 != null) {
                    byte[] remotePubKeyBytes = Base64.getDecoder().decode(remotePubKeyBase64);
                    remotePubKey = KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(remotePubKeyBytes));
                }
                
                if (remotePubKey != null) {
                    byte[] sharedSecret = P2PCryptoUtil.computeSharedSecret(ecdhKeyPair.getPrivate(), remotePubKey);
                    SecretKey aesKey = P2PCryptoUtil.deriveAESKey(sharedSecret);
                    SecretKey hmacKey = P2PCryptoUtil.deriveHMACKey(sharedSecret);
                    P2PSession session = new P2PSession(aesKey, hmacKey, ecdhKeyPair);
                    session.setRemoteECDHPublicKey(remotePubKey);
                    sessionMap.put(invite.getFrom(), session);
                }
                
                Long lastTime = lastInviteTime.get(invite.getFrom());
                long currentTime = System.currentTimeMillis();
                boolean isReconnect = lastTime != null && (currentTime - lastTime) < INVITE_COOLDOWN;
                
                // Генерируем список портов для приёма фрагментов ДО отправки ответа
                List<Integer> myPorts = generatePortsForP2P();
                // Сохраняем наши порты для этого контакта ДО отправки ответа
                myPortsMap.put(invite.getFrom(), myPorts);
                System.out.println("[P2PInviteManager] Сохранены наши порты для " + invite.getFrom() + ": " + myPorts);
                
                if (isReconnect) {
                    System.out.println("Received reconnect request from " + invite.getFrom());
                    inviteListener.onReconnectRequest(invite.getFrom(), invite.getIp(), invite.getPort());
                    
                    P2PInviteResponse inviteResponse = new P2PInviteResponse(
                        currentUser.getUsername(),
                        invite.getFrom(),
                        true,
                        myPubKeyBase64,
                        myPorts
                    );
                    String jsonResponse = objectMapper.writeValueAsString(inviteResponse);
                    writer.println(jsonResponse);
                    System.out.println("Auto-accepted reconnect request from " + invite.getFrom());
                    
                    lastInviteTime.put(invite.getFrom(), currentTime);
                    return;
                }
                
                CompletableFuture<Boolean> response = showInviteDialog(invite);
                boolean accepted = response.get();
                
                lastInviteTime.put(invite.getFrom(), currentTime);
                
                P2PInviteResponse inviteResponse = new P2PInviteResponse(
                    currentUser.getUsername(),
                    invite.getFrom(),
                    accepted,
                    myPubKeyBase64,
                    myPorts
                );
                
                String jsonResponse = objectMapper.writeValueAsString(inviteResponse);
                writer.println(jsonResponse);
                System.out.println("Sent response: " + jsonResponse);
                
                if (accepted) {
                    // Сначала уведомляем о принятии инвайта
                    System.out.println("[P2PInviteManager] Вызываем onInviteAccepted для " + invite.getFrom());
                    inviteListener.onInviteAccepted(invite.getFrom(), invite.getIp(), invite.getPort());
                    
                    // Затем сохраняем порты собеседника
                    List<Integer> remotePortsFromInvite = invite.getPorts();
                    if (remotePortsFromInvite != null) {
                        System.out.println("[P2PInviteManager] Получен список портов собеседника (из инвайта): " + remotePortsFromInvite);
                        remotePortsMap.put(invite.getFrom(), remotePortsFromInvite);
                    }
                } else {
                    inviteListener.onInviteRejected(invite.getFrom());
                }
                
            } catch (Exception e) {
                System.err.println("Error handling invite: " + e.getMessage());
                e.printStackTrace();
            } finally {
                try { socket.close(); } catch (IOException e) { e.printStackTrace(); }
            }
        });
    }

    private CompletableFuture<Boolean> showInviteDialog(P2PInvite invite) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("P2P Invite");
            alert.setHeaderText("New P2P Chat Invite");
            alert.setContentText(invite.getFrom() + " wants to start a P2P chat with you. Accept?");

            alert.showAndWait().ifPresent(response -> {
                future.complete(response == ButtonType.OK);
            });
        });
        return future;
    }

    public Map<String, P2PSession> getSessionMap() {
        return sessionMap;
    }

    public boolean sendInvite(String targetUsername, String targetIp, int targetPort) {
        if (targetIp == null || targetPort <= 0) {
            System.err.println("Invalid target IP or port for " + targetUsername);
            return false;
        }
        try (Socket socket = new Socket(targetIp, targetPort);
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            // Генерируем ECDH-ключи для этой сессии
            KeyPair ecdhKeyPair = P2PCryptoUtil.generateECDHKeyPair();
            String myPubKeyBase64 = Base64.getEncoder().encodeToString(ecdhKeyPair.getPublic().getEncoded());

            // Генерируем список портов для приёма фрагментов
            List<Integer> myPorts = generatePortsForP2P();
            // Сохраняем наши порты для этого контакта
            myPortsMap.put(targetUsername, myPorts);
            System.out.println("[P2PInviteManager] Сохранены наши порты для " + targetUsername + ": " + myPorts);

            // Формируем инвайт с публичным ключом и портами
            P2PInvite invite = new P2PInvite(
                currentUser.getUsername(),
                targetUsername,
                socket.getLocalAddress().getHostAddress(),
                INVITE_PORT,
                myPubKeyBase64,
                myPorts
            );

            String jsonInvite = objectMapper.writeValueAsString(invite);
            System.out.println("Sending invite to " + targetUsername + " at " + targetIp + ":" + targetPort);
            System.out.println("Invite content: " + jsonInvite);
            writer.println(jsonInvite);

            String jsonResponse = reader.readLine();
            if (jsonResponse == null) {
                System.err.println("No response received from " + targetUsername);
                return false;
            }

            System.out.println("Received response: " + jsonResponse);
            P2PInviteResponse response = objectMapper.readValue(jsonResponse, P2PInviteResponse.class);

            // Получаем публичный ключ собеседника из ответа
            String remotePubKeyBase64 = response.getEcdhPublicKeyBase64();
            PublicKey remotePubKey = null;
            if (remotePubKeyBase64 != null) {
                byte[] remotePubKeyBytes = Base64.getDecoder().decode(remotePubKeyBase64);
                remotePubKey = KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(remotePubKeyBytes));
            }

            // Сохраняем remotePorts для дальнейшей отправки сообщений
            List<Integer> remotePorts = response.getPorts();
            if (remotePorts != null) {
                System.out.println("[P2PInviteManager] Получен список портов собеседника: " + remotePorts);
                remotePortsMap.put(targetUsername, remotePorts);
            }

            // Вычисляем общий секрет и сессионные ключи
            if (remotePubKey != null) {
                byte[] sharedSecret = P2PCryptoUtil.computeSharedSecret(ecdhKeyPair.getPrivate(), remotePubKey);
                SecretKey aesKey = P2PCryptoUtil.deriveAESKey(sharedSecret);
                SecretKey hmacKey = P2PCryptoUtil.deriveHMACKey(sharedSecret);
                P2PSession session = new P2PSession(aesKey, hmacKey, ecdhKeyPair);
                session.setRemoteECDHPublicKey(remotePubKey);
                sessionMap.put(targetUsername, session);
            }

            boolean accepted = response.isAccepted();
            if (accepted && inviteListener != null) {
                System.out.println("[P2PInviteManager] Вызываем onInviteAccepted для " + targetUsername + " (отправитель инвайта)");
                inviteListener.onInviteAccepted(targetUsername, targetIp, targetPort);
            }

            return accepted;
        } catch (Exception e) {
            System.err.println("Error sending invite to " + targetUsername + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Генерирует список портов для P2P-соединения (например, 5 портов)
    public List<Integer> generatePortsForP2P() {
        List<Integer> ports = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            int port = 50000 + (int) (Math.random() * 10000);
            ports.add(port);
        }
        System.out.println("[P2PInviteManager] Сгенерированы порты для P2P: " + ports);
        return ports;
    }

    public void shutdown() {
        isRunning = false;
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }

    public void setInviteListener(P2PInviteListener listener) {
        System.out.println("[P2PInviteManager] Установка нового inviteListener");
        this.inviteListener = listener;
    }

    public void rejectInvite(String username) {
        try {
            // Отправляем сообщение об отклонении приглашения
            Socket socket = new Socket(username, INVITE_PORT);
            String rejectMessage = "REJECT:" + currentUser.getUsername();
            socket.getOutputStream().write(rejectMessage.getBytes(StandardCharsets.UTF_8));
            socket.close();
            
            if (inviteListener != null) {
                inviteListener.onInviteRejected(username);
            }
        } catch (IOException e) {
            System.err.println("Error rejecting invite from " + username + ": " + e.getMessage());
        }
    }

    public List<Integer> getRemotePorts(String username) {
        List<Integer> ports = remotePortsMap.get(username);
        System.out.println("[P2PInviteManager] Запрос портов для " + username + ": " + ports);
        System.out.println("[P2PInviteManager] Текущее состояние remotePortsMap: " + remotePortsMap);
        return ports;
    }

    // Добавляем метод для получения наших портов
    public List<Integer> getMyPortsForContact(String username) {
        List<Integer> ports = myPortsMap.get(username);
        System.out.println("[P2PInviteManager] Запрос НАШИХ портов для " + username + ": " + ports);
        System.out.println("[P2PInviteManager] Текущее состояние myPortsMap: " + myPortsMap);
        return ports;
    }
} 
package org.example.messengermrsocks.Networks;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.example.messengermrsocks.model.Messages.P2PInvite;
import org.example.messengermrsocks.model.Messages.P2PInviteResponse;
import org.example.messengermrsocks.model.Peoples.User;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class P2PInviteManager {
    private static final int INVITE_PORT = 9000;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final User currentUser;
    private final P2PInviteListener inviteListener;
    private volatile boolean isRunning = true;

    public interface P2PInviteListener {
        void onInviteAccepted(String fromUsername, String fromIp, int fromPort);
        void onInviteRejected(String fromUsername);
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
                    System.err.println("Received null invite from " + socket.getInetAddress());
                    return;
                }

                System.out.println("Received invite: " + jsonInvite);
                P2PInvite invite = objectMapper.readValue(jsonInvite, P2PInvite.class);
                
                // Show invite dialog
                CompletableFuture<Boolean> response = showInviteDialog(invite);
                boolean accepted = response.get(); // Wait for user response
                
                // Send response
                P2PInviteResponse inviteResponse = new P2PInviteResponse(
                    currentUser.getUsername(),
                    invite.getFrom(),
                    accepted
                );
                
                String jsonResponse = objectMapper.writeValueAsString(inviteResponse);
                writer.println(jsonResponse);
                System.out.println("Sent response: " + jsonResponse);
                
                if (accepted) {
                    inviteListener.onInviteAccepted(invite.getFrom(), invite.getIp(), invite.getPort());
                } else {
                    inviteListener.onInviteRejected(invite.getFrom());
                }
                
            } catch (Exception e) {
                System.err.println("Error handling invite: " + e.getMessage());
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
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

    public boolean sendInvite(String targetUsername, String targetIp, int targetPort) {
        if (targetIp == null || targetPort <= 0) {
            System.err.println("Invalid target IP or port for " + targetUsername);
            return false;
        }

        try (Socket socket = new Socket(targetIp, targetPort);
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            
            // Create and send invite
            P2PInvite invite = new P2PInvite(
                currentUser.getUsername(),
                targetUsername,
                socket.getLocalAddress().getHostAddress(),
                INVITE_PORT
            );
            
            String jsonInvite = objectMapper.writeValueAsString(invite);
            System.out.println("Sending invite to " + targetUsername + " at " + targetIp + ":" + targetPort);
            System.out.println("Invite content: " + jsonInvite);
            writer.println(jsonInvite);
            
            // Wait for response
            String jsonResponse = reader.readLine();
            if (jsonResponse == null) {
                System.err.println("No response received from " + targetUsername);
                return false;
            }
            
            System.out.println("Received response: " + jsonResponse);
            P2PInviteResponse response = objectMapper.readValue(jsonResponse, P2PInviteResponse.class);
            return response.isAccepted();
            
        } catch (Exception e) {
            System.err.println("Error sending invite to " + targetUsername + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
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
} 
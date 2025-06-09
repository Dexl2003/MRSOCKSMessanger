package org.example.messengermrsocks.Networks;

import org.example.messengermrsocks.model.Messages.Message;
import org.example.messengermrsocks.model.Peoples.Contact;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class P2PConnectionManager {
    private static final int MAX_FRAGMENT_SIZE = 1024; // 1KB per fragment
    private static final int MIN_PORT = 49152; // Dynamic port range start
    private static final int MAX_PORT = 65535; // Dynamic port range end
    private final Map<Contact, Set<Integer>> activePorts = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final AtomicInteger messageCounter = new AtomicInteger(0);

    public P2PConnectionManager() {
        startPortListener();
    }

    private void startPortListener() {
        executorService.submit(() -> {
            try (ServerSocket serverSocket = new ServerSocket(0)) {
                int basePort = serverSocket.getLocalPort();
                System.out.println("Base port for P2P: " + basePort);
                
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        handleIncomingConnection(clientSocket);
                    } catch (IOException e) {
                        if (!Thread.currentThread().isInterrupted()) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void handleIncomingConnection(Socket socket) {
        executorService.submit(() -> {
            try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
                Object received = in.readObject();
                if (received instanceof Message) {
                    handleIncomingMessage((Message) received);
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        });
    }

    private void handleIncomingMessage(Message message) {
        // TODO: Implement message handling logic
        System.out.println("Received message: " + message.getText());
    }

    public boolean sendMessage(Message message, Contact contact) {
        try {
            // Generate unique message ID
            int messageId = messageCounter.incrementAndGet();
            
            // Fragment message
            List<byte[]> fragments = fragmentMessage(message);
            
            // Generate ports for fragments
            List<Integer> ports = generatePorts(fragments.size());
            message.setListNextPort(ports);
            
            // Send fragments asynchronously
            List<CompletableFuture<Boolean>> sendFutures = new ArrayList<>();
            
            for (int i = 0; i < fragments.size(); i++) {
                final int fragmentIndex = i;
                final int port = ports.get(i);
                
                CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        return sendFragment(fragments.get(fragmentIndex), contact.getIp(), port);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }
                }, executorService);
                
                sendFutures.add(future);
            }
            
            // Wait for all fragments to be sent
            CompletableFuture.allOf(sendFutures.toArray(new CompletableFuture[0])).join();
            
            // Check if all fragments were sent successfully
            return sendFutures.stream().allMatch(CompletableFuture::join);
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private List<byte[]> fragmentMessage(Message message) throws IOException {
        List<byte[]> fragments = new ArrayList<>();
        byte[] messageData = serializeMessage(message);
        
        for (int i = 0; i < messageData.length; i += MAX_FRAGMENT_SIZE) {
            int end = Math.min(i + MAX_FRAGMENT_SIZE, messageData.length);
            byte[] fragment = Arrays.copyOfRange(messageData, i, end);
            fragments.add(fragment);
        }
        
        return fragments;
    }

    private List<Integer> generatePorts(int count) {
        List<Integer> ports = new ArrayList<>();
        Random random = new Random();
        
        for (int i = 0; i < count; i++) {
            int port;
            do {
                port = MIN_PORT + random.nextInt(MAX_PORT - MIN_PORT);
            } while (ports.contains(port));
            ports.add(port);
        }
        
        return ports;
    }

    private boolean sendFragment(byte[] fragment, String ip, int port) throws IOException {
        try (Socket socket = new Socket(ip, port);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            out.writeObject(fragment);
            return true;
        }
    }

    private byte[] serializeMessage(Message message) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(message);
            return baos.toByteArray();
        }
    }

    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }
} 
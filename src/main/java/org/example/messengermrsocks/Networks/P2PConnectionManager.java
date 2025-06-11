package org.example.messengermrsocks.Networks;

import org.example.messengermrsocks.model.Messages.Message;
import org.example.messengermrsocks.model.Peoples.Contact;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

public class P2PConnectionManager {
    private static final int MAX_FRAGMENT_SIZE = 1024; // 1KB per fragment
    private static final int MIN_PORT = 49152; // Dynamic port range start
    private static final int MAX_PORT = 65535; // Dynamic port range end
    private static final int SEND_TIMEOUT_SECONDS = 120; // 2 минуты таймаут
    public final Map<Contact, Set<Integer>> activePorts = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final AtomicInteger messageCounter = new AtomicInteger(0);
    private final P2PInviteManager p2pInviteManager;
    private final Map<String, P2PMessageReceiver> receiverMap = new ConcurrentHashMap<>();
    private BiConsumer<String, byte[]> onMessageReceived;
    private final Map<Integer, ServerSocket> activeListeners = new ConcurrentHashMap<>();
    private final SendManager sendManager;

    public P2PConnectionManager(P2PInviteManager p2pInviteManager) {
        this.p2pInviteManager = p2pInviteManager;
        this.sendManager = new SendManager(p2pInviteManager);
    }

    public void setOnMessageReceived(BiConsumer<String, byte[]> callback) {
        this.onMessageReceived = callback;
    }

    public boolean isContactConnected(Contact contact) {
        return contact != null && activePorts.containsKey(contact) && !activePorts.get(contact).isEmpty();
    }

    public void addContact(Contact contact) {
        activePorts.putIfAbsent(contact, new HashSet<>());
    }

    public void removeContact(Contact contact) {
        activePorts.remove(contact);
        receiverMap.remove(contact.getName());
    }

    public boolean sendMessage(Message message, Contact contact) {
        try {
            // Получаем IP адрес контакта
            String ip = contact.getIp();
            if (ip == null || ip.isEmpty()) {
                System.err.println("[P2PConnectionManager] IP адрес контакта не указан");
                return false;
            }

            // Получаем защищённую сессию для контакта
            P2PSession session = p2pInviteManager.getSessionMap().get(contact.getName());
            if (session == null) {
                System.err.println("[P2PConnectionManager] Сессия для контакта " + contact.getName() + " не найдена");
                return false;
            }
            P2PMessageSender sender = new P2PMessageSender(session.getAesKey(), session.getHmacKey());

            // Проверяем тип сообщения
            if ("media".equals(message.getType()) && message.getPayload() != null) {
                // Для медиафайлов проверяем размер
                byte[] mediaBytes = java.util.Base64.getDecoder().decode(message.getPayload());
                if (mediaBytes.length > 10 * 1024 * 1024) { // 10MB limit
                    System.err.println("[P2PConnectionManager] Размер файла превышает 10MB");
                    return false;
                }
                System.out.println("[P2PConnectionManager] Отправка медиафайла размером " + mediaBytes.length + " байт");
            }

            // Сериализуем сообщение
            byte[] messageBytes = serializeMessage(message);

            // Генерируем уникальный messageId
            int messageId = messageCounter.incrementAndGet();

            // Фрагментируем и шифруем
            List<P2PFragment> fragments = sender.fragmentAndEncrypt(messageBytes, messageId);
            System.out.println("[P2PConnectionManager] Сообщение разбито на " + fragments.size() + " фрагментов");

            // Получаем remotePorts для контакта
            List<Integer> ports = p2pInviteManager.getRemotePorts(contact.getName());
            if (ports == null) {
                ports = generatePorts(fragments.size());
            } else if (ports.size() < fragments.size()) {
                ports = generatePorts(fragments.size());
            } else if (ports.size() > fragments.size()) {
                ports = ports.subList(0, fragments.size());
            }
            message.setListNextPort(ports);

            // Отправляем фрагменты асинхронно
            CompletableFuture<Boolean> sendFuture = sendManager.sendFragmentsAsync(fragments, ip, ports);
            try {
                boolean result = sendFuture.get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (result) {
                    message.setSent(true);
                    if (message.getType().equals("media")) {
                        System.out.println("[P2PConnectionManager] Медиафайл успешно отправлен");
                    }
                }
                return result;
            } catch (TimeoutException e) {
                System.err.println("[P2PConnectionManager] Таймаут при отправке сообщения (превышено " + SEND_TIMEOUT_SECONDS + " секунд)");
                return false;
            }
        } catch (Exception e) {
            System.err.println("[P2PConnectionManager] Ошибка при отправке сообщения: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private byte[] serializeMessage(Message message) throws IOException {
        try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
             java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(baos)) {
            oos.writeObject(message);
            return baos.toByteArray();
        }
    }

    public List<Integer> generatePorts(int count) {
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

    public void startListenersForPorts(List<Integer> ports) {
        if (ports == null || ports.isEmpty()) {
            return;
        }
        for (Integer port : ports) {
            if (port == null) {
                continue;
            }
            if (activeListeners.containsKey(port)) {
                continue;
            }
            try {
                // Проверяем, не занят ли порт
                try (ServerSocket testSocket = new ServerSocket(port)) {
                    testSocket.close();
                } catch (IOException e) {
                    continue;
                }
                // Создаем основной ServerSocket
                ServerSocket serverSocket = new ServerSocket(port);
                activeListeners.put(port, serverSocket);
                executorService.submit(() -> {
                    try {
                        while (!Thread.currentThread().isInterrupted() && !serverSocket.isClosed()) {
                            try {
                                Socket clientSocket = serverSocket.accept();
                                handleIncomingConnection(clientSocket);
                            } catch (IOException e) {
                                if (!Thread.currentThread().isInterrupted() && !serverSocket.isClosed()) {
                                    e.printStackTrace();
                                }
                                break;
                            }
                        }
                    } finally {
                        if (!serverSocket.isClosed()) {
                            try {
                                serverSocket.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        activeListeners.remove(port);
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleIncomingConnection(Socket clientSocket) {
        try (ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream())) {
            Object obj = ois.readObject();
            if (obj instanceof P2PFragment) {
                P2PFragment fragment = (P2PFragment) obj;
                handleIncomingFragment(clientSocket.getInetAddress().getHostAddress(), fragment);
            } else {
                System.err.println("Получен неизвестный объект: " + obj.getClass().getName());
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Ошибка при обработке входящего подключения: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Ошибка при закрытии клиентского сокета: " + e.getMessage());
            }
        }
    }

    private void handleIncomingFragment(String senderIp, P2PFragment fragment) {
        try {
            Contact senderContact = null;
            for (Contact contact : activePorts.keySet()) {
                if (contact.getIp().equals(senderIp)) {
                    senderContact = contact;
                    break;
                }
            }
            if (senderContact == null) {
                return;
            }
            // Получаем сессию по имени пользователя
            P2PSession session = p2pInviteManager.getSessionMap().get(senderContact.getName());
            if (session == null) {
                return;
            }
            // Удаляем старый receiver, чтобы не было ошибок HMAC
            receiverMap.remove(senderContact.getName());
            receiverMap.computeIfAbsent(senderContact.getName(), 
                k -> new P2PMessageReceiver(session.getHmacKey().getEncoded(), session.getAesKey().getEncoded()));
            P2PMessageReceiver receiver = receiverMap.get(senderContact.getName());
            receiver.receiveFragment(fragment);
            byte[] messageBytes = receiver.tryAssembleMessage(fragment.getMessageId());
            if (messageBytes != null && onMessageReceived != null) {
                Message message = deserializeMessage(messageBytes);
                if (message != null) {
                    onMessageReceived.accept(senderIp, messageBytes);
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка обработки фрагмента: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String bytesToHex(byte[] bytes, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(bytes.length, length); i++) {
            sb.append(String.format("%02X ", bytes[i]));
        }
        return sb.toString();
    }

    private Message deserializeMessage(byte[] data) {
        try {
            // Используем Java сериализацию, так как данные уже дешифрованы
            try (java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(data);
                 java.io.ObjectInputStream ois = new java.io.ObjectInputStream(bais)) {
                return (Message) ois.readObject();
            }
        } catch (Exception e) {
            System.err.println("Ошибка десериализации: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public void shutdown() {
        // Останавливаем все активные слушатели
        for (Integer port : new ArrayList<>(activeListeners.keySet())) {
            stopListener(port);
        }
        // Останавливаем executor service
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }

    public SendManager getSendManager() {
        return sendManager;
    }

    public boolean isPortActive(Integer port) {
        if (port == null) {
            return false;
        }
        ServerSocket serverSocket = activeListeners.get(port);
        boolean isActive = serverSocket != null && !serverSocket.isClosed();
        return isActive;
    }

    public void stopListener(int port) {
        ServerSocket serverSocket = activeListeners.remove(port);
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
} 
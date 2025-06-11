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
    private final Map<Contact, Set<Integer>> activePorts = new ConcurrentHashMap<>();
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
    }

    public boolean sendMessage(Message message, Contact contact) {
        try {
            // Получаем IP адрес контакта
            String ip = contact.getIp();
            if (ip == null || ip.isEmpty()) {
                System.err.println("Cannot send message: contact IP is not set");
                return false;
            }

            // Получаем защищённую сессию для контакта
            P2PSession session = p2pInviteManager.getSessionMap().get(contact.getName());
            if (session == null) {
                System.err.println("No secure session for contact: " + contact.getName());
                return false;
            }
            P2PMessageSender sender = new P2PMessageSender(session.getAesKey(), session.getHmacKey());

            // Сериализуем сообщение
            byte[] messageBytes = serializeMessage(message);

            // Генерируем уникальный messageId
            int messageId = messageCounter.incrementAndGet();

            // Фрагментируем и шифруем
            List<P2PFragment> fragments = sender.fragmentAndEncrypt(messageBytes, messageId);
            System.out.println("[P2PConnectionManager] Количество фрагментов сообщения: " + fragments.size());

            // Получаем remotePorts для контакта
            List<Integer> ports = p2pInviteManager.getRemotePorts(contact.getName());
            System.out.println("[P2PConnectionManager] Полученные порты для контакта: " + ports);
            if (ports == null) {
                System.err.println("[P2PConnectionManager] Нет remotePorts для контакта, fallback на генерацию портов!");
                ports = generatePorts(fragments.size());
                System.out.println("[P2PConnectionManager] Сгенерированы новые порты: " + ports);
            } else if (ports.size() < fragments.size()) {
                System.err.println("[P2PConnectionManager] Недостаточно портов (портов: " + ports.size() + ", фрагментов: " + fragments.size() + "), fallback на генерацию портов!");
                ports = generatePorts(fragments.size());
                System.out.println("[P2PConnectionManager] Сгенерированы новые порты: " + ports);
            } else if (ports.size() > fragments.size()) {
                // Если портов больше чем фрагментов, берем только нужное количество портов
                System.out.println("[P2PConnectionManager] Портов больше чем фрагментов, используем первые " + fragments.size() + " портов");
                ports = ports.subList(0, fragments.size());
            }
            message.setListNextPort(ports);

            // Отправляем фрагменты асинхронно
            CompletableFuture<Boolean> sendFuture = sendManager.sendFragmentsAsync(fragments, ip, ports);
            return sendFuture.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.println("Error sending message: " + e.getMessage());
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
        System.out.println("[P2PConnectionManager] startListenersForPorts вызван с портами: " + ports);
        if (ports == null || ports.isEmpty()) {
            System.err.println("[P2PConnectionManager] Получен пустой список портов!");
            return;
        }
        
        for (Integer port : ports) {
            if (port == null) {
                System.err.println("[P2PConnectionManager] Получен null порт!");
                continue;
            }
            
            if (activeListeners.containsKey(port)) {
                System.out.println("[P2PConnectionManager] Слушатель для порта " + port + " уже активен");
                continue;
            }
            
            System.out.println("[P2PConnectionManager] Запуск слушателя на порту " + port);
            try {
                // Проверяем, не занят ли порт
                try (ServerSocket testSocket = new ServerSocket(port)) {
                    testSocket.close();
                } catch (IOException e) {
                    System.err.println("[P2PConnectionManager] Порт " + port + " уже занят: " + e.getMessage());
                    continue;
                }
                
                // Создаем основной ServerSocket
                ServerSocket serverSocket = new ServerSocket(port);
                System.out.println("[P2PConnectionManager] ServerSocket успешно создан для порта " + port);
                activeListeners.put(port, serverSocket);

                executorService.submit(() -> {
                    System.out.println("[P2PConnectionManager] Поток слушателя запущен для порта " + port);
                    try {
                        while (!Thread.currentThread().isInterrupted() && !serverSocket.isClosed()) {
                            try {
                                System.out.println("[P2PConnectionManager] Ожидание подключения на порту " + port);
                                Socket clientSocket = serverSocket.accept();
                                System.out.println("[P2PConnectionManager] Получено подключение на порту " + port + " от " + clientSocket.getInetAddress());
                                handleIncomingConnection(clientSocket);
                            } catch (IOException e) {
                                if (!Thread.currentThread().isInterrupted() && !serverSocket.isClosed()) {
                                    System.err.println("[P2PConnectionManager] Ошибка при принятии подключения на порту " + port + ": " + e.getMessage());
                                    e.printStackTrace();
                                }
                                break;
                            }
                        }
                    } finally {
                        System.out.println("[P2PConnectionManager] Поток слушателя завершён для порта " + port);
                        if (!serverSocket.isClosed()) {
                            try {
                                serverSocket.close();
                                System.out.println("[P2PConnectionManager] ServerSocket закрыт для порта " + port);
                            } catch (IOException e) {
                                System.err.println("[P2PConnectionManager] Ошибка при закрытии ServerSocket на порту " + port + ": " + e.getMessage());
                            }
                        }
                        activeListeners.remove(port);
                    }
                });
            } catch (IOException e) {
                System.err.println("[P2PConnectionManager] Ошибка при создании ServerSocket на порту " + port + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void stopListener(int port) {
        ServerSocket serverSocket = activeListeners.remove(port);
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                System.out.println("[P2PConnectionManager] Слушатель на порту " + port + " остановлен");
            } catch (IOException e) {
                System.err.println("[P2PConnectionManager] Ошибка при остановке слушателя на порту " + port + ": " + e.getMessage());
            }
        }
    }

    private void handleIncomingConnection(Socket clientSocket) {
        System.out.println("[P2PConnectionManager] Обработка входящего подключения от " + clientSocket.getInetAddress());
        try (ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream())) {
            Object obj = ois.readObject();
            if (obj instanceof P2PFragment) {
                P2PFragment fragment = (P2PFragment) obj;
                System.out.println("[P2PConnectionManager] Получен фрагмент сообщения: messageId=" + fragment.getMessageId() + ", fragmentIndex=" + fragment.getFragmentIndex());
                handleIncomingFragment(clientSocket.getInetAddress().getHostAddress(), fragment);
            } else {
                System.err.println("[P2PConnectionManager] Получен неизвестный объект: " + obj.getClass().getName());
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[P2PConnectionManager] Ошибка при обработке входящего подключения: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("[P2PConnectionManager] Ошибка при закрытии клиентского сокета: " + e.getMessage());
            }
        }
    }

    private void handleIncomingFragment(String senderIp, P2PFragment fragment) {
        try {
            System.out.println("[P2PConnectionManager] handleIncomingFragment: sender=" + senderIp + ", messageId=" + fragment.getMessageId() + ", fragmentIndex=" + fragment.getFragmentIndex());
            
            // Находим контакт по IP
            Contact senderContact = null;
            for (Contact contact : activePorts.keySet()) {
                if (contact.getIp().equals(senderIp)) {
                    senderContact = contact;
                    break;
                }
            }
            
            if (senderContact == null) {
                System.err.println("[P2PConnectionManager] Не найден контакт для IP: " + senderIp);
                return;
            }
            
            // Получаем сессию по имени пользователя
            P2PSession session = p2pInviteManager.getSessionMap().get(senderContact.getName());
            if (session == null) {
                System.err.println("[P2PConnectionManager] Нет сессии для отправителя: " + senderContact.getName());
                return;
            }
            
            receiverMap.computeIfAbsent(senderContact.getName(), 
                k -> new P2PMessageReceiver(session.getHmacKey().getEncoded(), session.getAesKey().getEncoded()));
            P2PMessageReceiver receiver = receiverMap.get(senderContact.getName());
            receiver.receiveFragment(fragment);
            byte[] messageBytes = receiver.tryAssembleMessage(fragment.getMessageId());
            if (messageBytes != null && onMessageReceived != null) {
                System.out.println("[P2PConnectionManager] Сообщение собрано полностью, размер: " + messageBytes.length + " байт");
                System.out.println("[P2PConnectionManager] Первые 16 байт сообщения: " + bytesToHex(messageBytes, 16));
                Message message = deserializeMessage(messageBytes);
                if (message != null) {
                    onMessageReceived.accept(senderIp, messageBytes);
                }
            }
        } catch (Exception e) {
            System.err.println("[P2PConnectionManager] Ошибка обработки фрагмента: " + e.getMessage());
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
        System.out.println("[P2PConnectionManager] Попытка десериализации сообщения. Размер данных: " + data.length);
        System.out.println("[P2PConnectionManager] Первые 16 байт данных: " + bytesToHex(data, 16));
        try {
            // Используем Java сериализацию, так как данные уже дешифрованы
            try (java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(data);
                 java.io.ObjectInputStream ois = new java.io.ObjectInputStream(bais)) {
                return (Message) ois.readObject();
            }
        } catch (Exception e) {
            System.err.println("[P2PConnectionManager] Ошибка десериализации: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public void shutdown() {
        System.out.println("[P2PConnectionManager] Начало процесса остановки...");
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
        System.out.println("[P2PConnectionManager] Процесс остановки завершен");
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
        System.out.println("[P2PConnectionManager] Проверка активности порта " + port + ": " + isActive);
        return isActive;
    }
} 
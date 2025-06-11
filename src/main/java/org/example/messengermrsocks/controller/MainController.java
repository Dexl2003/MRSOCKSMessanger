package org.example.messengermrsocks.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.control.ListCell;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.example.messengermrsocks.model.Peoples.Contact;
import java.io.IOException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.example.messengermrsocks.model.Messages.Message;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javafx.application.Platform;
import org.example.messengermrsocks.model.Messages.HistoryMessages;
import org.example.messengermrsocks.model.Peoples.User;
import java.util.HashMap;
import java.util.Map;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.paint.Color;
import java.util.HashSet;
import java.util.Set;
import javafx.stage.FileChooser;
import java.io.File;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import org.example.messengermrsocks.storage.SecureLocalStorageService;
import java.util.List;
import java.util.ArrayList;
import org.example.messengermrsocks.model.Messages.MessengerData;
import org.example.messengermrsocks.Networks.AuthManager;
import javafx.stage.Stage;
import org.example.messengermrsocks.Networks.P2PManager;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonBar;
import org.example.messengermrsocks.Networks.P2PInviteManager;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import javafx.scene.control.ProgressBar;

public class MainController {
    @FXML private ListView<Contact> ListViewDialog;
    @FXML private ListView<Message> listViewHistoryChat;
    @FXML private TextField searchField;
    @FXML private Label chatNameLabel;
    @FXML private Label chatTimeLabel;
    @FXML private Label connectionStatusLabel;
    @FXML private TextArea sendTextField;
    @FXML private Button sendButton;
    @FXML private Button disconnectButton;
    @FXML private Button attachButton;
    @FXML private ListView<User> userSearchResults;
    @FXML private Button repeatButton;
    @FXML private ProgressBar uploadProgressBar;

    private Map<Contact, HistoryMessages> messageHistories;
    private HistoryMessages currentHistory;
    private User currentUser;
    private Contact currentContact;
    private Set<Contact> disconnectedContacts = new HashSet<>();
    private ObservableList<Contact> allContacts;
    private ObservableList<Contact> filteredContacts;
    public static ListView listViewHistoryChatStatic;
    private MessengerData localData;
    private AuthManager authManager;
    private Map<Contact, String> draftMessages = new HashMap<>();
    private P2PManager p2pManager;
    private static final String MEDIA_DIR = "user_data/media/";
    private static final int UPLOAD_TIMEOUT_SECONDS = 120; // 2 минуты таймаут

    static {
        try {
            Files.createDirectories(Path.of(MEDIA_DIR));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setAuthManager(AuthManager authManager) {
        this.authManager = authManager;
        this.currentUser = authManager.getCurrentUser();
        initializeP2PManager();
        initialize();
    }

    public void initialize() {
        if (currentUser == null) {
            return; // Не инициализируем, если пользователь не установлен
        }

        // --- Загрузка данных из локального хранилища ---
        try {
            MessengerData loaded = SecureLocalStorageService.loadData(MessengerData.class, currentUser);
            if (loaded != null) {
                localData = loaded;
            } else {
                localData = new MessengerData(currentUser);
            }
        } catch (Exception e) {
            e.printStackTrace();
            localData = new MessengerData(currentUser);
        }

        // --- Привязка к UI ---
        allContacts = FXCollections.observableArrayList(localData.getContacts());
        filteredContacts = FXCollections.observableArrayList(allContacts);
        ListViewDialog.setItems(filteredContacts);

        // --- Восстановление историй сообщений ---
        messageHistories = new HashMap<>();
        for (Contact contact : allContacts) {
            List<Message> messages = localData.getDialogs().getOrDefault(contact.getName(), new ArrayList<>());
            HistoryMessages history = new HistoryMessages(currentUser, contact);
            for (Message m : messages) history.addMessage(m);
            messageHistories.put(contact, history);
        }

        // --- Восстановление отключённых диалогов ---
        disconnectedContacts = new HashSet<>();
        Set<String> disconnectedNames = localData.getDisconnectedContactsNames();
        for (Contact contact : allContacts) {
            if (disconnectedNames.contains(contact.getName())) {
                disconnectedContacts.add(contact);
            }
        }

        // --- Восстановление P2P соединений при запуске ---
        for (Contact contact : allContacts) {
            if (!disconnectedContacts.contains(contact)) {
                new Thread(() -> {
                    boolean success = p2pManager.requestP2PChannel(contact);
                    // Можно обновить UI по результату, если нужно
                }).start();
            }
        }

        // --- Обработка поиска ---
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String filter = newVal == null ? "" : newVal.trim().toLowerCase();
            ObservableList<Contact> filtered = allContacts.filtered(contact -> contact.getName().toLowerCase().contains(filter));
            filteredContacts.setAll(filtered);

            // Если среди локальных контактов ничего не найдено и строка поиска не пуста — ищем на сервере
            if (filtered.isEmpty() && !filter.isEmpty()) {
                new Thread(() -> {
                    List<User> foundUsers = authManager.searchUsers(filter);
                    Platform.runLater(() -> {
                        userSearchResults.setItems(FXCollections.observableArrayList(foundUsers));
                        userSearchResults.setVisible(true);
                    });
                }).start();
            } else {
                userSearchResults.setItems(FXCollections.observableArrayList());
                userSearchResults.setVisible(false);
            }
        });

        // --- Обработчик кнопки disconnect ---
        disconnectButton.setOnAction(e -> handleDisconnect());
        disconnectButton.setDisable(true); // Изначально кнопка неактивна
        
        // --- Кастомный cell factory для контактов ---
        ListViewDialog.setCellFactory(listView -> new ListCell<Contact>() {
            @Override
            protected void updateItem(Contact contact, boolean empty) {
                super.updateItem(contact, empty);
                if (empty || contact == null) {
                    setGraphic(null);
                    setContextMenu(null);
                } else {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/contact-list-cell.fxml"));
                        Parent root = loader.load();
                        org.example.messengermrsocks.controller.ContactListCellController cellController = loader.getController();

                        cellController.nameLabel.setText(contact.getName());
                        cellController.timeLabel.setText(contact.getTime());
                        
                        // Новый способ: статус отдельным Label
                        if (disconnectedContacts.contains(contact)) {
                            cellController.statusLabel.setVisible(true);
                            cellController.statusLabel.setManaged(true);
                        } else {
                            cellController.statusLabel.setVisible(false);
                            cellController.statusLabel.setManaged(false);
                        }

                        if (contact.getAvatarUrl() != null && !contact.getAvatarUrl().isEmpty()) {
                            cellController.avatarImageView.setImage(
                                new Image(getClass().getResourceAsStream(contact.getAvatarUrl()))
                            );
                        }
                        setGraphic(root);

                        // --- Контекстное меню ---
                        javafx.scene.control.ContextMenu contextMenu = new javafx.scene.control.ContextMenu();
                        javafx.scene.control.MenuItem disconnectItem = new javafx.scene.control.MenuItem("Отключить диалог");
                        javafx.scene.control.MenuItem restoreItem = new javafx.scene.control.MenuItem("Восстановить диалог");
                        javafx.scene.control.MenuItem deleteItem = new javafx.scene.control.MenuItem("Удалить контакт");

                        disconnectItem.setOnAction(e -> {
                            currentContact = contact;
                            disconnectDialog();
                        });
                        restoreItem.setOnAction(e -> {
                            currentContact = contact;
                            restoreDialog();
                        });
                        deleteItem.setOnAction(e -> {
                            // Подтверждение удаления
                            Alert alert = new Alert(AlertType.CONFIRMATION);
                            alert.setTitle("Удаление контакта");
                            alert.setHeaderText("Удалить контакт?");
                            alert.setContentText("Вы уверены, что хотите удалить контакт '" + contact.getName() + "' и всю историю сообщений?");
                            alert.showAndWait().ifPresent(response -> {
                                if (response == ButtonType.OK) {
                                    allContacts.remove(contact);
                                    filteredContacts.remove(contact);
                                    messageHistories.remove(contact);
                                    disconnectedContacts.remove(contact);
                                    // Если удаляем текущий контакт, сбрасываем отображение
                                    if (currentContact == contact) {
                                        listViewHistoryChat.setItems(FXCollections.observableArrayList());
                                        chatNameLabel.setText("");
                                        chatTimeLabel.setText("");
                                    }
                                    saveLocalData(); // Автоматическое сохранение
                                    // После локального удаления:
                                    p2pManager.getP2pInviteManager().sendDeleteContact(contact.getIp(), currentUser.getUsername());
                                }
                            });
                        });

                        // Только нужные пункты меню
                        if (disconnectedContacts.contains(contact)) {
                            contextMenu.getItems().addAll(restoreItem, deleteItem);
                        } else {
                            contextMenu.getItems().addAll(disconnectItem, deleteItem);
                        }
                        setContextMenu(contextMenu);

                    } catch (IOException e) {
                        e.printStackTrace();
                        setGraphic(null);
                        setContextMenu(null);
                    }
                }
            }
        });

        // --- Обработка выбора контакта ---
        ListViewDialog.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (oldVal != null) {
                // Сохраняем черновик для предыдущего контакта
                draftMessages.put(oldVal, sendTextField.getText());
            }
            if (newVal != null) {
                searchField.clear();
                // Восстанавливаем черновик для нового контакта
                String draft = draftMessages.getOrDefault(newVal, "");
                sendTextField.setText(draft);
                // Сброс выделения, чтобы избежать IndexOutOfBoundsException
                Platform.runLater(() -> ListViewDialog.getSelectionModel().clearSelection());
                currentContact = newVal;
                if (disconnectedContacts.contains(currentContact)) {
                    // Если диалог был отключен, предлагаем восстановить
                    Alert alert = new Alert(AlertType.CONFIRMATION);
                    alert.setTitle("Восстановление диалога");
                    alert.setHeaderText("Восстановить диалог?");
                    alert.setContentText("Хотите восстановить отключенный диалог с " + currentContact.getName() + "?");

                    alert.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.OK) {
                            restoreDialog();
                        }
                    });
                } else {
                    updateDialogView(newVal);
                }
            }
        });

        // --- Обработка отправки сообщения ---
        sendButton.setOnAction(e -> sendMessage());
        sendTextField.setOnKeyPressed(event -> handleSendTextAreaKey(event));

        // --- Кастомный cell factory для истории сообщений ---
        listViewHistoryChat.setCellFactory(listView -> new ListCell<Message>() {
            @Override
            protected void updateItem(Message message, boolean empty) {
                super.updateItem(message, empty);
                if (empty || message == null) {
                    setGraphic(null);
                } else {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/message-list-cell.fxml"));
                        HBox root = loader.load();
                        org.example.messengermrsocks.controller.MessageListCellController cellController = loader.getController();
                        
                        // Устанавливаем базовые стили для сообщения
                        cellController.messageTextLabel.setStyle("-fx-background-color: #fff; -fx-background-radius: 8; -fx-padding: 8; -fx-text-fill: #222; -fx-font-size: 20;");
                        cellController.timeLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 12;");
                        
                        // Обновляем содержимое
                        cellController.messageTextLabel.setText(message.getText());
                        cellController.timeLabel.setText(message.getTime());
                        
                        // Update message status
                        cellController.updateMessageStatus(message);
                        
                        if (message.getAvatarUrl() != null) {
                            cellController.avatarImageView.setImage(new Image(getClass().getResourceAsStream(message.getAvatarUrl())));
                        }
                        
                        // Динамически меняем порядок элементов для выравнивания
                        root.getChildren().clear();
                        Pane spacer = new Pane();
                        HBox.setHgrow(spacer, Priority.ALWAYS);
                        VBox messageVBox = (VBox) cellController.messageTextLabel.getParent();
                        
                        if (message.isOwn()) {
                            cellController.avatarImageView.setVisible(false);
                            root.getChildren().addAll(spacer, messageVBox);
                            // Стили для собственных сообщений
                            cellController.messageTextLabel.setStyle("-fx-background-color: #dcf8c6; -fx-background-radius: 8; -fx-padding: 8; -fx-text-fill: #222; -fx-font-size: 20;");
                        } else {
                            cellController.avatarImageView.setVisible(true);
                            root.getChildren().addAll(cellController.avatarImageView, messageVBox, spacer);
                            // Стили для сообщений собеседника
                            cellController.messageTextLabel.setStyle("-fx-background-color: #fff; -fx-background-radius: 8; -fx-padding: 8; -fx-text-fill: #222; -fx-font-size: 20;");
                        }
                        
                        setGraphic(root);
                    } catch (IOException e) {
                        e.printStackTrace();
                        setGraphic(null);
                    }
                }
            }
        });

        // --- Обработчик кнопки-скрепки ---
        attachButton.setOnAction(e -> handleAttachFile());

        // --- Drag & Drop для диалога ---
        listViewHistoryChat.setOnDragOver(event -> {
            if (event.getGestureSource() != listViewHistoryChat && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });
        listViewHistoryChat.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles() && currentHistory != null && !disconnectedContacts.contains(currentContact)) {
                for (File file : db.getFiles()) {
                    addMediaMessage(file);
                }
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });

        listViewHistoryChatStatic = listViewHistoryChat;

        // --- Слушатель ширины для динамического предпросмотра медиа ---
        listViewHistoryChat.widthProperty().addListener((obs, oldVal, newVal) -> {
            listViewHistoryChat.refresh();
        });

        // Добавляем обработчик закрытия окна для вызова logout и shutdown P2P
        Platform.runLater(() -> {
            Stage stage = (Stage) ListViewDialog.getScene().getWindow();
            stage.setOnCloseRequest(event -> {
                // Сначала закрываем все P2P соединения
                if (p2pManager != null) {
                    System.out.println("Shutting down P2P connections...");
                    shutdown();
                }
                // Затем выполняем logout
                if (authManager != null) {
                    System.out.println("Logging out...");
                    authManager.logoutFromApi();
                }
                // Сохраняем последнее состояние
                saveLocalData();
            });
        });

        // Обработчик выбора пользователя из результатов поиска
        userSearchResults.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                createChatWithUser(newVal);
                searchField.clear();
                userSearchResults.setItems(FXCollections.observableArrayList());
                userSearchResults.setVisible(false);
            }
        });

        repeatButton.setOnAction(e -> handleRepeatButton());
    }

    private void initializeP2PManager() {
        p2pManager = new P2PManager(currentUser);
        
        // Устанавливаем слушатель для обновления UI при реконнекте
        p2pManager.setReconnectListener((fromUsername, fromIp, fromPort) -> {
            Platform.runLater(() -> {
                Contact existingContact = allContacts.stream()
                    .filter(c -> c.getName().equals(fromUsername))
                    .findFirst()
                    .orElse(null);

                if (existingContact != null) {
                    existingContact.setIp(fromIp);
                    existingContact.setMainPort(fromPort);
                    existingContact.setConnected(true);
                    if (currentContact != null && currentContact.getName().equals(fromUsername)) {
                        updateConnectionStatus(true);
                    }
                }
            });
        });

        // Устанавливаем слушатель для обновления UI при получении сообщения
        p2pManager.getP2pConnectionManager().setOnMessageReceived((senderIp, messageBytes) -> {
            try {
                Message message = deserializeMessage(messageBytes);
                if (message != null) {
                    Platform.runLater(() -> {
                        System.out.println("[onMessageReceived] Получено сообщение от IP: " + senderIp);
                        System.out.println("[onMessageReceived] mainText (имя): " + message.getMainText());
                        System.out.println("[onMessageReceived] text: " + message.getText());
                        System.out.println("[onMessageReceived] Текущий список контактов: ");
                        for (Contact c : allContacts) {
                            System.out.println("  - " + c.getName() + " (ip: " + c.getIp() + ")");
                        }
                        Contact contact = allContacts.stream()
                            .filter(c -> c.getName().equals(message.getMainText()))
                            .findFirst()
                            .orElse(null);
                        if (contact == null) {
                            String senderName = message.getMainText();
                            System.out.println("[onMessageReceived] Контакт не найден по имени. Имя для создания: '" + senderName + "'");
                            if (senderName == null || senderName.trim().isEmpty()) {
                                System.out.println("[onMessageReceived] Имя отсутствует, контакт не будет создан!");
                                return;
                            }
                            contact = new Contact(senderName, message.getTime(), "/images/image.png");
                            contact.setIp(senderIp);
                            allContacts.add(contact);
                            filteredContacts.add(contact);
                            messageHistories.put(contact, new HistoryMessages(currentUser, contact));
                            updateDialogView(contact);
                            ListViewDialog.getSelectionModel().select(contact);
                            System.out.println("[onMessageReceived] Новый контакт создан и добавлен: " + senderName);
                        } else {
                            if (!contact.getIp().equals(senderIp)) {
                                System.out.println("[onMessageReceived] Обновляю IP контакта: " + contact.getName() + " с " + contact.getIp() + " на " + senderIp);
                                contact.setIp(senderIp);
                            }
                        }
                        message.setOwn(false);
                        HistoryMessages history = messageHistories.get(contact);
                        if (history == null) {
                            System.out.println("[onMessageReceived] История для контакта не найдена, создаю новую.");
                            history = new HistoryMessages(currentUser, contact);
                            messageHistories.put(contact, history);
                        }
                        history.addMessage(message);
                        message.setReceived(true);
                        System.out.println("[onMessageReceived] Сообщение добавлено в историю контакта: " + contact.getName());
                        if (currentContact == null || currentContact.getName().equals(contact.getName())) {
                            currentContact = contact;
                            updateDialogView(contact);
                            System.out.println("[onMessageReceived] Обновлен текущий контакт и UI: " + contact.getName());
                        }
                        listViewHistoryChat.setItems(history.getMessages());
                        listViewHistoryChat.scrollTo(listViewHistoryChat.getItems().size() - 1);
                        saveLocalData();
                        System.out.println("[onMessageReceived] История сообщений обновлена и сохранена.");
                    });
                }
            } catch (Exception e) {
                System.err.println("[MainController] Ошибка при обработке входящего сообщения: " + e.getMessage());
                e.printStackTrace();
            }
        });

        // Устанавливаем слушатель для обновления UI при принятии инвайта
        p2pManager.setUIListener(new P2PInviteManager.P2PInviteListener() {
            @Override
            public void onInviteAccepted(String fromUsername, String fromIp, int fromPort) {
                Platform.runLater(() -> {
                    Contact existingContact = allContacts.stream()
                        .filter(c -> c.getName().equals(fromUsername))
                        .findFirst()
                        .orElse(null);

                    if (existingContact == null) {
                        createNewContactDialog(fromUsername, fromIp, fromPort);
                    } else {
                        existingContact.setIp(fromIp);
                        existingContact.setMainPort(fromPort);
                        existingContact.setConnected(true);
                        if (currentContact != null && currentContact.getName().equals(fromUsername)) {
                            updateConnectionStatus(true);
                            disconnectButton.setDisable(false);
                            sendButton.setDisable(false);
                            sendTextField.setDisable(false);
                        }
                    }
                });
            }

            @Override
            public void onInviteRejected(String fromUsername) {
                Platform.runLater(() -> {
                    Contact contact = allContacts.stream()
                        .filter(c -> c.getName().equals(fromUsername))
                        .findFirst()
                        .orElse(null);
                    if (contact != null) {
                        contact.setConnected(false);
                        if (currentContact != null && currentContact.getName().equals(fromUsername)) {
                            updateConnectionStatus(false);
                            showAlert("Disconnected by companion", "Connection with " + fromUsername + " was terminated by the companion.");
                            disconnectButton.setDisable(true);
                            sendButton.setDisable(true);
                            sendTextField.setDisable(true);
                        }
                    }
                });
            }

            @Override
            public void onReconnectRequest(String fromUsername, String fromIp, int fromPort) {
                Platform.runLater(() -> {
                    if (currentContact != null && currentContact.getName().equals(fromUsername)) {
                        updateConnectionStatus(true);
                        disconnectButton.setDisable(false);
                        sendButton.setDisable(false);
                        sendTextField.setDisable(false);
                    }
                });
            }

            @Override
            public void onContactDeleted(String fromUsername) {
                Platform.runLater(() -> {
                    Contact contactToDelete = allContacts.stream()
                        .filter(c -> c.getName().equals(fromUsername))
                        .findFirst()
                        .orElse(null);
                    if (contactToDelete != null) {
                        allContacts.remove(contactToDelete);
                        filteredContacts.remove(contactToDelete);
                        messageHistories.remove(contactToDelete);
                        disconnectedContacts.remove(contactToDelete);
                        saveLocalData();
                        if (currentContact == contactToDelete) {
                            listViewHistoryChat.setItems(FXCollections.observableArrayList());
                            chatNameLabel.setText("");
                            chatTimeLabel.setText("");
                        }
                    }
                });
            }
        });
    }

    private void updateConnectionStatus(boolean isConnected) {
        if (currentContact != null) {
            currentContact.setConnected(isConnected);
            Platform.runLater(() -> {
                if (connectionStatusLabel != null) {
                    connectionStatusLabel.setText(isConnected ? "Подключено" : "Отключено");
                    connectionStatusLabel.setStyle(isConnected ? 
                        "-fx-text-fill: green;" : "-fx-text-fill: red;");
                    
                    // Показываем/скрываем кнопку повтора в зависимости от статуса
                    repeatButton.setVisible(!isConnected);
                    
                    // Обновляем состояние кнопок
                    disconnectButton.setDisable(!isConnected);
                    sendButton.setDisable(!isConnected);
                    sendTextField.setDisable(!isConnected);
                }
            });
        }
    }

    private void handleDisconnect() {
        if (currentContact != null) {
            // Отправляем сигнал отключения собеседнику
            p2pManager.getP2pInviteManager().sendDisconnect(currentContact.getIp(), currentUser.getUsername());
            // Удаляем контакт из P2P соединения
            p2pManager.getP2pConnectionManager().removeContact(currentContact);
            // Удаляем сессию из P2PInviteManager
            p2pManager.getP2pInviteManager().getSessionMap().remove(currentContact.getName());
            currentContact.setConnected(false);
            updateConnectionStatus(false);
            showAlert("Disconnected", 
                "Connection with " + currentContact.getName() + " has been terminated.");
            // Блокируем ввод и отправку сообщений для инициатора разрыва
            disconnectButton.setDisable(true);
            sendButton.setDisable(true);
            sendTextField.setDisable(true);
        }
    }

    private void disconnectDialog() {
        if (currentContact == null) return;
        // Отключаем диалог
        disconnectedContacts.add(currentContact);
        listViewHistoryChat.setItems(FXCollections.observableArrayList());
        chatNameLabel.setText("Диалог отключен");
        chatNameLabel.setStyle("-fx-text-fill: red; -fx-font-size: 18;");
        chatTimeLabel.setText("");
        connectionStatusLabel.setText("Disconnected");
        connectionStatusLabel.setStyle("-fx-text-fill: #f44336; -fx-font-size: 12;");
        disconnectButton.setDisable(true);
        sendButton.setDisable(true);
        sendTextField.setDisable(true);
        // Обновляем отображение в списке контактов
        ListViewDialog.refresh();
        saveLocalData();
    }

    private void restoreDialog() {
        if (currentContact == null) return;
        disconnectedContacts.remove(currentContact);
        updateDialogView(currentContact);
        // Обновляем отображение в списке контактов
        ListViewDialog.refresh();
        // Пытаемся восстановить P2P соединение
        if (p2pManager != null) {
            new Thread(() -> {
                boolean success = p2pManager.requestP2PChannel(currentContact);
                Platform.runLater(() -> {
                    updateConnectionStatus(success);
                    if (!success) {
                        Alert alert = new Alert(AlertType.WARNING);
                        alert.setTitle("Ошибка P2P соединения");
                        alert.setHeaderText("Не удалось восстановить P2P соединение");
                        alert.setContentText("Попытка восстановить P2P соединение с " + currentContact.getName() + " не удалась.");
                        alert.showAndWait();
                    }
                });
            }).start();
        }
        saveLocalData();
    }

    private void updateDialogView(Contact contact) {
        chatNameLabel.setText(contact.getName());
        chatTimeLabel.setText(contact.getTime());
        currentHistory = messageHistories.get(contact);
        if (currentHistory == null) {
            currentHistory = new HistoryMessages(currentUser, contact);
            messageHistories.put(contact, currentHistory);
        }
        listViewHistoryChat.setItems(currentHistory.getMessages());
        disconnectButton.setDisable(false);
        sendButton.setDisable(false);
        sendTextField.setDisable(false);
        // Обновляем стили заголовка
        chatNameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18;");
        chatTimeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14;");
        
        // Используем статус из контакта
        updateConnectionStatus(contact.isConnected());
        
        // Автоскролл к последнему сообщению при переключении диалога
        Platform.runLater(() -> {
            if (listViewHistoryChat.getItems().size() > 0) {
                listViewHistoryChat.scrollTo(listViewHistoryChat.getItems().size() - 1);
            }
        });
    }

    private void sendMessage() {
        String text = sendTextField.getText();
        if (text != null && !text.isEmpty() && currentHistory != null && !disconnectedContacts.contains(currentContact)) {
            // Add new message to local history
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            Message message = new Message(text, time, "/images/image.png", true);
            currentHistory.addMessage(message);
            javafx.application.Platform.runLater(() -> listViewHistoryChat.scrollTo(listViewHistoryChat.getItems().size() - 1));
            saveLocalData();

            // --- Реальная отправка через SendManager ---
            if (p2pManager != null && currentContact != null) {
                System.out.println("[MainController] Отправка сообщения через SendManager...");
                boolean sent = p2pManager.getP2pConnectionManager().getSendManager().sendMessage(text, currentContact);
                System.out.println("[MainController] sendMessage вернул: " + sent);
            }

            sendTextField.clear();
        }
    }

    private void handleAttachFile() {
        if (currentHistory == null || disconnectedContacts.contains(currentContact)) return;
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите медиа-файл");
        File file = fileChooser.showOpenDialog(attachButton.getScene().getWindow());
        if (file != null) {
            addMediaMessage(file);
        }
    }

    private void addMediaMessage(File file) {
        if (file == null || !file.exists()) {
            System.err.println("[MainController] Файл не существует");
            return;
        }

        try {
            // Проверяем размер файла
            long fileSize = file.length();
            if (fileSize > 10 * 1024 * 1024) { // 10MB limit
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("Ошибка");
                alert.setHeaderText("Файл слишком большой");
                alert.setContentText("Максимальный размер файла - 10MB");
                alert.showAndWait();
                return;
            }

            String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
            boolean isImage = file.getName().toLowerCase().matches(".*\\.(png|jpg|jpeg|gif|bmp)$");
            String text = isImage ? "" : "[Медиа] " + file.getName();
            
            // Создаем сообщение
            Message message = new Message(text, time, "/images/image.png", true, file.getAbsolutePath());
            message.setType("media");
            
            // Читаем файл и кодируем в base64
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            message.setPayload(java.util.Base64.getEncoder().encodeToString(fileBytes));
            
            // Добавляем сообщение в историю
            currentHistory.addMessage(message);
            Platform.runLater(() -> {
                listViewHistoryChat.scrollTo(listViewHistoryChat.getItems().size() - 1);
                uploadProgressBar.setVisible(true);
                uploadProgressBar.setProgress(0);
            });
            
            // Сохраняем изменения
            saveLocalData();
            
            // Отправляем через P2P в отдельном потоке
            if (p2pManager != null && currentContact != null) {
                new Thread(() -> {
                    try {
                        boolean sent = p2pManager.getP2pConnectionManager().sendMessage(message, currentContact);
                        Platform.runLater(() -> {
                            uploadProgressBar.setVisible(false);
                            if (sent) {
                                message.setSent(true);
                                listViewHistoryChat.refresh();
                            } else {
                                System.err.println("[MainController] Не удалось отправить медиафайл");
                                Alert alert = new Alert(AlertType.ERROR);
                                alert.setTitle("Ошибка отправки");
                                alert.setHeaderText("Не удалось отправить файл");
                                alert.setContentText("Попробуйте отправить файл еще раз");
                                alert.showAndWait();
                            }
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            uploadProgressBar.setVisible(false);
                            System.err.println("[MainController] Ошибка при отправке медиафайла: " + e.getMessage());
                            e.printStackTrace();
                            Alert alert = new Alert(AlertType.ERROR);
                            alert.setTitle("Ошибка");
                            alert.setHeaderText("Не удалось отправить файл");
                            alert.setContentText("Произошла ошибка: " + e.getMessage());
                            alert.showAndWait();
                        });
                    }
                }).start();
            }
        } catch (Exception e) {
            System.err.println("[MainController] Ошибка при подготовке медиафайла: " + e.getMessage());
            e.printStackTrace();
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Ошибка");
            alert.setHeaderText("Не удалось подготовить файл");
            alert.setContentText("Произошла ошибка: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void handleSendTextAreaKey(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            if (event.isShiftDown()) {
                // Перенос строки
                sendTextField.appendText("\n");
            } else {
                // Отправка сообщения только если оно не пустое
                String text = sendTextField.getText().trim();
                if (!text.isEmpty()) sendMessage();
            }
            event.consume();
        }
    }

    private void searchUsers(String query) {
        new Thread(() -> {
            List<User> users = authManager.searchUsers(query);
            Platform.runLater(() -> {
                userSearchResults.setItems(FXCollections.observableArrayList(users));
            });
        }).start();
    }

    private void createChatWithUser(User user) {
        // Проверяем, нет ли уже чата с этим пользователем
        for (Contact contact : allContacts) {
            if (contact.getName().equals(user.getUsername())) {
                // Если чат уже существует, просто выбираем его
                ListViewDialog.getSelectionModel().select(contact);
                return;
            }
        }

        // Создаем новый контакт
        String currentTime = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        Contact newContact = new Contact(user.getUsername(), currentTime, "/images/image.png");
        boolean isOnline = "online".equalsIgnoreCase(user.getStatus());
        newContact.setStatus(isOnline);
        
        // Устанавливаем IP и порт для P2P
        newContact.setIp(user.getIp());
        newContact.setMainPort(9000); // Используем фиксированный порт
        
        // Добавляем контакт в список
        allContacts.add(newContact);
        filteredContacts.add(newContact);
        // Создаем историю сообщений
        HistoryMessages history = new HistoryMessages(currentUser, newContact);
        messageHistories.put(newContact, history);
        // Сохраняем изменения
        saveLocalData();
        // Выбираем новый чат
        ListViewDialog.getSelectionModel().select(newContact);

        // Отправляем P2P инвайт
        if (p2pManager != null && newContact.getIp() != null) {
            new Thread(() -> {
                boolean success = p2pManager.requestP2PChannel(newContact);
                if (!success) {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(AlertType.WARNING);
                        alert.setTitle("Ошибка P2P соединения");
                        alert.setHeaderText("Не удалось установить P2P соединение");
                        alert.setContentText("Попытка установить P2P соединение с " + newContact.getName() + " не удалась. Возможно, пользователь оффлайн или недоступен.");
                        alert.showAndWait();
                    });
                }
            }).start();
        }
    }

    // После любого изменения контактов или сообщений вызываем:
    private void saveLocalData() {
        try {
            // Сохраняем актуальные контакты
            localData.getContacts().clear();
            localData.getContacts().addAll(allContacts);
            // Сохраняем актуальные диалоги
            localData.getDialogs().clear();
            for (Contact c : allContacts) {
                HistoryMessages h = messageHistories.get(c);
                if (h != null) localData.getDialogs().put(c.getName(), new ArrayList<>(h.getMessages()));
            }
            // Сохраняем отключённые диалоги
            Set<String> disconnectedNames = new HashSet<>();
            for (Contact c : disconnectedContacts) {
                disconnectedNames.add(c.getName());
            }
            localData.setDisconnectedContactsNames(disconnectedNames);
            SecureLocalStorageService.saveData(localData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRepeatButton() {
        if (currentContact != null) {
            // Показываем статус попытки подключения
            connectionStatusLabel.setText("Подключение...");
            connectionStatusLabel.setStyle("-fx-text-fill: orange;");
            
            // Запускаем попытку подключения в отдельном потоке
            new Thread(() -> {
                boolean success = p2pManager.requestP2PChannel(currentContact);
                
                // Обновляем UI в главном потоке
                Platform.runLater(() -> {
                    if (success) {
                        connectionStatusLabel.setText("Подключено");
                        connectionStatusLabel.setStyle("-fx-text-fill: green;");
                        currentContact.setConnected(true);
                        disconnectButton.setDisable(false);
                        sendButton.setDisable(false);
                        sendTextField.setDisable(false);
                        repeatButton.setVisible(false);
                    } else {
                        connectionStatusLabel.setText("Ошибка подключения");
                        connectionStatusLabel.setStyle("-fx-text-fill: red;");
                        currentContact.setConnected(false);
                        disconnectButton.setDisable(true);
                        sendButton.setDisable(true);
                        sendTextField.setDisable(true);
                        repeatButton.setVisible(true);
                        
                        // Показываем диалог с ошибкой
                        Alert alert = new Alert(AlertType.ERROR);
                        alert.setTitle("Ошибка подключения");
                        alert.setHeaderText("Не удалось подключиться к пользователю");
                        alert.setContentText("Проверьте, что пользователь онлайн и доступен. Вы можете попробовать подключиться снова, нажав кнопку 'Повторить'.");
                        alert.showAndWait();
                    }
                });
            }).start();
        }
    }

    // Добавляем метод для принудительного закрытия всех соединений
    public void shutdown() {
        if (p2pManager != null) {
            // Отправляем DISCONNECT всем активным контактам
            for (Contact contact : allContacts) {
                if (contact.isConnected()) {
                    p2pManager.getP2pInviteManager().sendDisconnect(contact.getIp(), currentUser.getUsername());
                }
            }
            p2pManager.shutdown();
        }
        // Сохраняем состояние перед выходом
        saveLocalData();
    }

    private void handleAutoReconnect(Contact contact) {
        if (contact != null) {
            updateConnectionStatus(true);
            showAlert("Connection Restored", 
                "Connection with " + contact.getName() + " has been restored automatically.");
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void createNewContactDialog(String username, String ip, int port) {
        // Проверяем, есть ли уже контакт с таким именем
        Contact existingContact = allContacts.stream()
            .filter(c -> c.getName().equals(username))
            .findFirst()
            .orElse(null);
        if (existingContact != null) {
            // Если контакт уже есть, просто выбираем его и обновляем статус
            Platform.runLater(() -> {
                if (!ListViewDialog.getItems().isEmpty()) {
                    ListViewDialog.getSelectionModel().select(existingContact);
                }
                updateDialogView(existingContact);
                existingContact.setIp(ip);
                existingContact.setMainPort(port);
                existingContact.setConnected(true);
                updateConnectionStatus(true);
            });
            return;
        }
        // Создаем новый контакт
        Contact newContact = new Contact(username,
            java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
            "/images/image.png");
        newContact.setIp(ip);
        newContact.setMainPort(port);
        
        // Создаем новый диалог
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("New Connection");
        dialog.setHeaderText("New connection request from " + username);
        
        // Добавляем кнопки
        ButtonType acceptButton = new ButtonType("Accept", ButtonBar.ButtonData.OK_DONE);
        ButtonType rejectButton = new ButtonType("Reject", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(acceptButton, rejectButton);

        // Обработка результата диалога
        dialog.showAndWait().ifPresent(response -> {
            if (response == acceptButton) {
                // Добавляем контакт в P2P менеджер
                p2pManager.getP2pConnectionManager().addContact(newContact);
                
                // Обновляем UI
                Platform.runLater(() -> {
                    allContacts.add(newContact);
                    filteredContacts.add(newContact);
                    HistoryMessages history = new HistoryMessages(currentUser, newContact);
                    messageHistories.put(newContact, history);
                    if (!ListViewDialog.getItems().isEmpty()) {
                        ListViewDialog.getSelectionModel().select(newContact);
                    }
                    updateDialogView(newContact);
                    newContact.setConnected(true);
                    updateConnectionStatus(true);
                    showAlert("Connection Established", 
                        "Connection with " + username + " has been established.");
                });
            } else {
                // Отклоняем подключение
                p2pManager.getP2pInviteManager().rejectInvite(username);
                allContacts.remove(newContact);
                filteredContacts.remove(newContact);
            }
        });
    }

    private Message deserializeMessage(byte[] data) {
        try (java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(data);
             java.io.ObjectInputStream ois = new java.io.ObjectInputStream(bais)) {
            return (Message) ois.readObject();
        } catch (Exception e) {
            System.err.println("[MainController] Ошибка при десериализации сообщения: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}

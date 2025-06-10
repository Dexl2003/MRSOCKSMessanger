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
    private P2PManager p2pManager;
    private Map<Contact, String> draftMessages = new HashMap<>();

    public void setAuthManager(AuthManager authManager) {
        this.authManager = authManager;
        this.currentUser = authManager.getCurrentUser();
        this.p2pManager = new P2PManager(currentUser);
        
        // Устанавливаем слушателя повторных подключений
        p2pManager.setReconnectListener((fromUsername, fromIp, fromPort) -> {
            Platform.runLater(() -> {
                // Находим контакт по имени пользователя
                Contact contact = allContacts.stream()
                    .filter(c -> c.getName().equals(fromUsername))
                    .findFirst()
                    .orElse(null);
                
                if (contact != null) {
                    // Обновляем IP и порт контакта
                    contact.setIp(fromIp);
                    contact.setMainPort(fromPort);
                    contact.setConnected(true);
                    
                    // Если это текущий выбранный контакт, обновляем статус
                    if (currentContact != null && currentContact.getName().equals(fromUsername)) {
                        updateConnectionStatus(true);
                        showAlert("Connection Restored", 
                            "Connection with " + fromUsername + " has been restored automatically.");
                    }
                } else {
                    // Если контакт не найден, создаем новый диалог
                    createNewContactDialog(fromUsername, fromIp, fromPort);
                }
            });
        });

        // Устанавливаем слушателя для новых подключений
        p2pManager.getP2pInviteManager().setInviteListener(new P2PInviteManager.P2PInviteListener() {
            @Override
            public void onInviteAccepted(String fromUsername, String fromIp, int fromPort) {
                Platform.runLater(() -> {
                    // Проверяем, существует ли уже контакт
                    Contact existingContact = allContacts.stream()
                        .filter(c -> c.getName().equals(fromUsername))
                        .findFirst()
                        .orElse(null);

                    if (existingContact == null) {
                        // Создаем новый диалог для нового контакта
                        createNewContactDialog(fromUsername, fromIp, fromPort);
                    } else {
                        // Обновляем существующий контакт
                        existingContact.setIp(fromIp);
                        existingContact.setMainPort(fromPort);
                        existingContact.setConnected(true);
                        if (currentContact != null && currentContact.getName().equals(fromUsername)) {
                            updateConnectionStatus(true);
                        }
                    }
                });
            }

            @Override
            public void onInviteRejected(String fromUsername) {
                Platform.runLater(() -> {
                    // Находим контакт и обновляем его статус
                    Contact contact = allContacts.stream()
                        .filter(c -> c.getName().equals(fromUsername))
                        .findFirst()
                        .orElse(null);
                    
                    if (contact != null) {
                        contact.setConnected(false);
                        if (currentContact != null && currentContact.getName().equals(fromUsername)) {
                            updateConnectionStatus(false);
                        }
                    }
                    
                    showAlert("Connection Rejected", 
                        "Connection request from " + fromUsername + " was rejected.");
                });
            }

            @Override
            public void onReconnectRequest(String fromUsername, String fromIp, int fromPort) {
                // Обработка повторных подключений уже реализована в ReconnectListener
            }
        });

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
                    p2pManager.shutdown();
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

    private void updateConnectionStatus(boolean isConnected) {
        if (currentContact != null) {
            currentContact.setConnected(isConnected);
            Platform.runLater(() -> {
                if (connectionStatusLabel != null) {
                    connectionStatusLabel.setText(isConnected ? "Connected" : "Disconnected");
                    connectionStatusLabel.setStyle(isConnected ? 
                        "-fx-text-fill: green;" : "-fx-text-fill: red;");
                }
            });
        }
    }

    private void handleDisconnect() {
        if (currentContact != null) {
            p2pManager.getP2pConnectionManager().removeContact(currentContact);
            currentContact.setConnected(false);
            updateConnectionStatus(false);
            showAlert("Disconnected", 
                "Connection with " + currentContact.getName() + " has been terminated.");
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
            // Add new message
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            Message message = new Message(text, time, "/images/image.png", true);
            currentHistory.addMessage(message);
            // Автоскролл к последнему сообщению
            javafx.application.Platform.runLater(() -> listViewHistoryChat.scrollTo(listViewHistoryChat.getItems().size() - 1));
            saveLocalData(); // Автоматическое сохранение
            // Simulate sending message and receiving status
            new Thread(() -> {
                try {
                    // Simulate network delay
                    Thread.sleep(1000);
                    // Update message status
                    message.setSent(true);
                    Platform.runLater(() -> listViewHistoryChat.refresh());
                    // Simulate receiving confirmation
                    Thread.sleep(1000);
                    message.setReceived(true);
                    Platform.runLater(() -> listViewHistoryChat.refresh());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
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
        String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        boolean isImage = file.getName().toLowerCase().matches(".*\\.(png|jpg|jpeg|gif|bmp)$");
        String text = isImage ? "" : "[Медиа] " + file.getName();
        Message message = new Message(text, time, "/images/image.png", true, file.getAbsolutePath());
        currentHistory.addMessage(message);
        // Автоскролл к последнему сообщению
        javafx.application.Platform.runLater(() -> listViewHistoryChat.scrollTo(listViewHistoryChat.getItems().size() - 1));
        saveLocalData(); // Автоматическое сохранение
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
        if (currentContact != null && p2pManager != null) {
            new Thread(() -> {
                boolean success = p2pManager.requestP2PChannel(currentContact);
                Platform.runLater(() -> {
                    updateConnectionStatus(success);
                    Alert alert = new Alert(success ? AlertType.INFORMATION : AlertType.WARNING);
                    alert.setTitle("P2P соединение");
                    alert.setHeaderText(success ? "P2P соединение установлено!" : "Не удалось установить P2P соединение");
                    alert.setContentText(success
                            ? "Соединение с " + currentContact.getName() + " успешно установлено."
                            : "Попытка установить P2P соединение с " + currentContact.getName() + " не удалась.");
                    alert.showAndWait();
                });
            }).start();
        }
    }

    // Добавляем метод для принудительного закрытия всех соединений
    public void shutdown() {
        if (p2pManager != null) {
            System.out.println("Shutting down all P2P connections...");
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
                ListViewDialog.getSelectionModel().select(existingContact);
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
                    ListViewDialog.getItems().add(newContact);
                    
                    // Создаем историю сообщений для нового контакта
                    HistoryMessages history = new HistoryMessages(currentUser, newContact);
                    messageHistories.put(newContact, history);
                    
                    // Выбираем новый контакт
                    ListViewDialog.getSelectionModel().select(newContact);
                    updateDialogView(newContact);
                    
                    // Устанавливаем статус соединения
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
}

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

public class MainController {
    @FXML private ListView<Contact> ListViewDialog;
    @FXML private ListView<Message> listViewHistoryChat;
    @FXML private TextField searchField;
    @FXML private Label chatNameLabel;
    @FXML private Label chatTimeLabel;
    @FXML private TextArea sendTextField;
    @FXML private Button sendButton;
    @FXML private Button disconnectButton;
    @FXML private Button attachButton;

    private Map<Contact, HistoryMessages> messageHistories;
    private HistoryMessages currentHistory;
    private User currentUser;
    private Contact currentContact;
    private Set<Contact> disconnectedContacts = new HashSet<>();
    private ObservableList<Contact> allContacts;
    private ObservableList<Contact> filteredContacts;
    public static ListView listViewHistoryChatStatic;
    private MessengerData localData;

    public void initialize() {
        // --- Загрузка данных из локального хранилища ---
        try {
            MessengerData loaded = SecureLocalStorageService.loadData(MessengerData.class);
            if (loaded != null) {
                localData = loaded;
            } else {
                int i = 1;
                for (Contact contact : localData.getContacts()) {
                    List<Message> messages = new ArrayList<>();
                    messages.add(new Message("Привет, это тестовое сообщение #" + i, "10:" + (10 + i), "/images/image.png", false));
                    messages.add(new Message("Ответ на тестовое сообщение #" + i, "10:" + (12 + i), "/images/image.png", true));
                    messages.add(new Message("Еще одно сообщение для " + contact.getName(), "10:" + (14 + i), "/images/image.png", false));
                    localData.getDialogs().put(contact.getName(), messages);
                    i++;
                }
                SecureLocalStorageService.saveData(localData);
            }
        } catch (Exception e) {
            e.printStackTrace();
            localData = new MessengerData();
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
            filteredContacts.setAll(
                allContacts.filtered(contact -> contact.getName().toLowerCase().contains(filter))
            );
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
            if (newVal != null) {
                searchField.clear();
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
    }

    private void handleDisconnect() {
        if (currentHistory == null || currentContact == null) {
            return;
        }

        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Подтверждение отключения");
        alert.setHeaderText("Отключить диалог?");
        alert.setContentText("Вы уверены, что хотите отключить диалог с " + currentContact.getName() + "? В будущем это действие также разорвет P2P соединение.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                disconnectDialog();
            }
        });
    }

    private void disconnectDialog() {
        if (currentContact == null) return;
        // Отключаем только текущий диалог
        disconnectedContacts.add(currentContact);
        listViewHistoryChat.setItems(FXCollections.observableArrayList());
        chatNameLabel.setText("Диалог отключен");
        chatNameLabel.setStyle("-fx-text-fill: red; -fx-font-size: 18;");
        chatTimeLabel.setText("");
        disconnectButton.setDisable(true);
        sendButton.setDisable(true);
        sendTextField.setDisable(true);
        // Обновляем отображение в списке контактов
        ListViewDialog.refresh();
        // В будущем здесь будет код для разрыва P2P соединения
        System.out.println("P2P соединение будет разорвано с " + currentContact.getName());
        saveLocalData(); // Автоматическое сохранение
    }

    private void restoreDialog() {
        if (currentContact == null) return;
        disconnectedContacts.remove(currentContact);
        updateDialogView(currentContact);
        // Обновляем отображение в списке контактов
        ListViewDialog.refresh();
        // В будущем здесь будет код для восстановления P2P соединения
        System.out.println("P2P соединение будет восстановлено с " + currentContact.getName());
        saveLocalData(); // Автоматическое сохранение
    }

    private void updateDialogView(Contact contact) {
        chatNameLabel.setText(contact.getName());
        chatTimeLabel.setText(contact.getTime());
        currentHistory = messageHistories.get(contact);
        listViewHistoryChat.setItems(currentHistory.getMessages());
        disconnectButton.setDisable(false);
        sendButton.setDisable(false);
        sendTextField.setDisable(false);
        // Обновляем стили заголовка
        chatNameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18;");
        chatTimeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14;");
        // Автоскролл к последнему сообщению при переключении диалога
        javafx.application.Platform.runLater(() -> {
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
                // Отправка сообщения
                sendMessage();
                event.consume();
            }
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
}

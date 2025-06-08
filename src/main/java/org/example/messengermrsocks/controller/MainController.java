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

public class MainController {

    @FXML
    private ListView<Contact> ListViewDialog;

    @FXML
    private ListView<Message> listViewHistoryChat;

    @FXML
    private TextField searchField;

    @FXML
    private Label chatNameLabel;

    @FXML
    private Label chatTimeLabel;

    @FXML
    private TextField sendTextField;

    @FXML
    private Button sendButton;

    private ObservableList<Message> messages;

    public void initialize() {
        // --- Кастомный cell factory для контактов ---
        ListViewDialog.setCellFactory(listView -> new ListCell<Contact>() {
            @Override
            protected void updateItem(Contact contact, boolean empty) {
                super.updateItem(contact, empty);
                if (empty || contact == null) {
                    setGraphic(null);
                } else {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/contact-list-cell.fxml"));
                        Parent root = loader.load();
                        org.example.messengermrsocks.controller.ContactListCellController cellController = loader.getController();
                        cellController.nameLabel.setText(contact.getName());
                        cellController.timeLabel.setText(contact.getTime());
                        if (contact.getAvatarUrl() != null && !contact.getAvatarUrl().isEmpty()) {
                            cellController.avatarImageView.setImage(
                                new Image(getClass().getResourceAsStream(contact.getAvatarUrl()))
                            );
                        }
                        setGraphic(root);
                    } catch (IOException e) {
                        e.printStackTrace();
                        setGraphic(null);
                    }
                }
            }
        });

        // --- Тестовые контакты ---
        ObservableList<Contact> contacts = FXCollections.observableArrayList(
                new Contact("Name User", "19:40", "/images/image.png"),
                new Contact("Alice", "18:22", "/images/image.png"),
                new Contact("Bob", "17:10", "/images/image.png")
        );
        ListViewDialog.setItems(contacts);

        // --- Обработка выбора контакта ---
        ListViewDialog.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                chatNameLabel.setText(newVal.getName());
                chatTimeLabel.setText(newVal.getTime());
                // Здесь можно обновлять сообщения чата
            }
        });

        // --- Обработка отправки сообщения ---
        sendButton.setOnAction(e -> sendMessage());
        sendTextField.setOnAction(e -> sendMessage());

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
                        cellController.messageTextLabel.setText(message.getText());
                        cellController.timeLabel.setText(message.getTime());
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
                        } else {
                            cellController.avatarImageView.setVisible(true);
                            root.getChildren().addAll(cellController.avatarImageView, messageVBox, spacer);
                        }
                        setGraphic(root);
                    } catch (IOException e) {
                        e.printStackTrace();
                        setGraphic(null);
                    }
                }
            }
        });

        // --- Тестовые сообщения ---
        messages = FXCollections.observableArrayList(
                new Message("Привет! Это моё сообщение.", "19:40", "/images/image.png", true),
                new Message("А это сообщение собеседника.", "19:41", "/images/image.png", false),
                new Message("Ещё одно моё сообщение.", "19:42", "/images/image.png", true)
        );
        listViewHistoryChat.setItems(messages);
    }

    private void sendMessage() {
        String text = sendTextField.getText();
        if (text != null && !text.isEmpty()) {
            // Добавляем новое сообщение пользователя в историю, время без секунд
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            messages.add(new Message(text, time, "/images/image.png", true));
            sendTextField.clear();
        }
    }

    private void viewHistoryChat(){

    }

}

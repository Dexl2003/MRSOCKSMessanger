package org.example.messengermrsocks.model.Messages;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.example.messengermrsocks.model.Peoples.Contact;
import org.example.messengermrsocks.model.Peoples.User;

public class HistoryMessages {
    private User user;
    private Contact contact;
    private ObservableList<Message> messages;

    public HistoryMessages(User user, Contact contact) {
        this.user = user;
        this.contact = contact;
        this.messages = FXCollections.observableArrayList();
    }

    public User getUser() {
        return user;
    }

    public Contact getContact() {
        return contact;
    }

    public ObservableList<Message> getMessages() {
        return messages;
    }

    public void addMessage(Message message) {
        messages.add(message);
    }
}

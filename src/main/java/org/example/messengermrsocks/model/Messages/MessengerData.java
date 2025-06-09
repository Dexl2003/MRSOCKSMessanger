package org.example.messengermrsocks.model.Messages;

import org.example.messengermrsocks.model.Peoples.Contact;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

public class MessengerData {
    private List<Contact> contacts = new ArrayList<>();
    private Map<String, List<Message>> dialogs = new HashMap<>(); // key: contact name
    private Set<String> disconnectedContactsNames = new HashSet<>();

    public List<Contact> getContacts() { return contacts; }
    public void setContacts(List<Contact> contacts) { this.contacts = contacts; }

    public Map<String, List<Message>> getDialogs() { return dialogs; }
    public void setDialogs(Map<String, List<Message>> dialogs) { this.dialogs = dialogs; }

    public Set<String> getDisconnectedContactsNames() { return disconnectedContactsNames; }
    public void setDisconnectedContactsNames(Set<String> disconnectedContactsNames) { this.disconnectedContactsNames = disconnectedContactsNames; }
} 
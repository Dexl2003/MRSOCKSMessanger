package org.example.messengermrsocks.Networks;

import javafx.collections.ObservableList;
import org.example.messengermrsocks.model.Peoples.Contact;
import org.example.messengermrsocks.model.intarfaces.ContactRepository;

public class ConnectorContactRepository implements ContactRepository {
    @Override
    public ObservableList<Contact> loadContacts(int authToken) {
        return null;
    }
}

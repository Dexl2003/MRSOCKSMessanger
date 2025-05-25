package org.example.messengermrsocks.model.intarfaces;

import javafx.collections.ObservableList;
import org.example.messengermrsocks.model.Peoples.Contact;

public interface ContactRepository {
    ObservableList<Contact> loadContacts(int authToken);

}

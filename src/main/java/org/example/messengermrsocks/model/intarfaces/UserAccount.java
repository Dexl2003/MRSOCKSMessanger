package org.example.messengermrsocks.model.intarfaces;

import org.example.messengermrsocks.model.Peoples.User;

public interface UserAccount {
    User loadUserAccount(int authToken);
}

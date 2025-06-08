package org.example.messengermrsocks.Networks;

import org.example.messengermrsocks.model.MetaData;
import org.example.messengermrsocks.model.Peoples.Contact;
import org.example.messengermrsocks.model.intarfaces.SendProvider;

public class SendManager implements SendProvider {
    @Override
    public MetaData questStatusContact(Contact contact) {
        return null;
    }

    @Override
    public String generateSelfCloseKey() {
        return "";
    }

    @Override
    public String generateSelfOpenKey(int param_G, String selfCloseKey) {
        return "";
    }

    @Override
    public void sendSelfOpenKeyToContact(String selfOpenKey) {

    }

    @Override
    public String generateGeneralCloseKey(String questOpenKey, String selfCloseKey) {
        return "";
    }
}

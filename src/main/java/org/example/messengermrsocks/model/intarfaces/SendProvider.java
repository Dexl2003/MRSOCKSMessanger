package org.example.messengermrsocks.model.intarfaces;

import org.example.messengermrsocks.model.MetaData;
import org.example.messengermrsocks.model.Peoples.Contact;

import java.util.List;

public interface SendProvider {
    MetaData questStatusContact(Contact contact);
    String generateSelfCloseKey();
    String generateSelfOpenKey(int param_G,String selfCloseKey);
    void sendSelfOpenKeyToContact(String selfOpenKey);
    String generateGeneralCloseKey(String questOpenKey, String selfCloseKey);
    default Boolean sendMessage(String text, Contact contact){
        MetaData metaData = questStatusContact(contact);
        String selfCloseKey = generateSelfCloseKey();
        String selfOpenKey = generateSelfOpenKey(metaData.getG(),selfCloseKey);
        sendSelfOpenKeyToContact(selfOpenKey);
        String generalCloseKey = generateGeneralCloseKey(metaData.getOpenKey(),selfCloseKey);
        boolean requestAcceptMessage = false;
        
        return requestAcceptMessage;
    }


}

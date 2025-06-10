module org.example.messengermrsocks {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires com.fasterxml.jackson.databind;
    requires org.slf4j;
    requires io.netty.transport;
    requires io.netty.buffer;
    requires com.google.gson;
    requires org.bouncycastle.provider;
    requires java.desktop;

    exports org.example.messengermrsocks.model.Messages to com.fasterxml.jackson.databind;
    exports org.example.messengermrsocks.model.Peoples to com.fasterxml.jackson.databind;
    opens org.example.messengermrsocks to javafx.fxml;
    exports org.example.messengermrsocks;
    exports org.example.messengermrsocks.controller;
    opens org.example.messengermrsocks.controller to javafx.fxml;
    opens org.example.messengermrsocks.model to com.fasterxml.jackson.databind;
}
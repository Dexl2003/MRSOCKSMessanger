module org.example.messengermrsocks {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;


    opens org.example.messengermrsocks to javafx.fxml;
    exports org.example.messengermrsocks;
    exports org.example.messengermrsocks.controller;
    opens org.example.messengermrsocks.controller to javafx.fxml;
}
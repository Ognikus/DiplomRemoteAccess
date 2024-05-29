module com.example.diplomremoteaccess {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.datatransfer;
    requires java.rmi;
    requires java.desktop;
    requires Java.WebSocket;

    opens com.example.diplomremoteaccess to javafx.fxml;
    exports com.example.diplomremoteaccess;
    exports com.example.diplomremoteaccess.controlller;
    opens com.example.diplomremoteaccess.controlller to  javafx.fxml;
}

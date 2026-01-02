module com.example.pixz {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires java.desktop;
    requires metadata.extractor;

    opens com.example.pixz to javafx.fxml;

    exports com.example.pixz;
}
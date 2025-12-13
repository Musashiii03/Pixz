module com.example.wingallery {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires java.desktop;

    opens com.example.wingallery to javafx.fxml;

    exports com.example.wingallery;
}
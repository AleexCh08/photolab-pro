module com.example.photolab {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires java.desktop;
    requires kotlin.stdlib;


    opens com.example.photolab to javafx.fxml;
    exports com.example.photolab;
}
module com.mik.icc.icceditor {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.mik.icc.icceditor to javafx.fxml;
    exports com.mik.icc.icceditor;
}
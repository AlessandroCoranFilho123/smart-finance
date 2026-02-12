module app {
    requires javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.graphics; // transitive?

    requires java.sql;
    requires java.logging;
    requires java.prefs;

    exports app;
    exports app.model;
    exports app.service;

    opens app.controller to javafx.fxml;
    opens app to javafx.fxml;
}
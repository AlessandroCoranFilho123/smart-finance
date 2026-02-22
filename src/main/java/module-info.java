module app {
    requires javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.graphics;

    requires java.sql;
    requires java.prefs;
    requires org.slf4j;

    exports app;
    exports app.model;
    exports app.service;
    exports app.controller;
    exports app.database;
    exports app.repository;
    exports app.util;
    exports app.view;

    opens app.controller to javafx.fxml;
    opens app to javafx.fxml;
    opens app.view to javafx.fxml;
    opens app.model to javafx.base;
}
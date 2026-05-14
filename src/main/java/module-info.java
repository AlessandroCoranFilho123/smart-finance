module app {
    requires javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.graphics;

    requires java.sql;
    requires java.prefs;
    requires com.sun.jna;
    requires com.sun.jna.platform;
    requires org.slf4j;

    exports app;
    exports app.model;
    exports app.service;
    exports app.controller;
    exports app.database;
    exports app.repository;
    exports app.security;
    exports app.util;
    exports app.view;

    opens app.controller to javafx.fxml;
    opens app to javafx.fxml;
    opens app.view to javafx.fxml;
    opens app.model to javafx.base;
}

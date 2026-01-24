module app {
    requires javafx.controls;
    requires javafx.graphics;
    requires java.logging;
    requires java.prefs;

    exports app;
    exports app.model;
    exports app.service;
}

module org.htech.expensecalculator {
    requires javafx.fxml;
    requires java.sql;
    requires org.controlsfx.controls;
    requires fontawesomefx;
    requires mysql.connector.j;
    requires javafx.controls;



    opens org.htech.expensecalculator.controllers to javafx.fxml;
    exports org.htech.expensecalculator.controllers;
    opens org.htech.expensecalculator.modal to javafx.base;
    exports org.htech.expensecalculator.main;
    opens org.htech.expensecalculator.main to javafx.fxml;
}
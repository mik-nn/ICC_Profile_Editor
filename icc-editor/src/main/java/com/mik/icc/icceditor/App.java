package com.mik.icc.icceditor;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        TabPane tabPane = new TabPane();

        Tab commonTab = new Tab("Common");
        commonTab.setContent(new StackPane());

        Tab mimakiTab = new Tab("Mimaki");
        mimakiTab.setContent(new StackPane());

        tabPane.getTabs().add(commonTab);
        tabPane.getTabs().add(mimakiTab);

        Scene scene = new Scene(tabPane, 640, 480);
        stage.setScene(scene);
        stage.setTitle("ICC Profile Editor");
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }

}

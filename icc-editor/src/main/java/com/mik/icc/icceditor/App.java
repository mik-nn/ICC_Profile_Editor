package com.mik.icc.icceditor;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class App extends Application {

    private Label filePathLabel = new Label("No file opened");

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();

        // Menu
        MenuBar menuBar = new MenuBar();
        Menu fileMenu = new Menu("File");
        MenuItem openMenuItem = new MenuItem("Open");
        openMenuItem.setOnAction(e -> openFile(stage));
        fileMenu.getItems().add(openMenuItem);
        menuBar.getMenus().add(fileMenu);
        root.setTop(menuBar);

        // Tabs
        TabPane tabPane = new TabPane();

        Tab commonTab = new Tab("Common");
        commonTab.setContent(new StackPane(filePathLabel));

        Tab mimakiTab = new Tab("Mimaki");
        mimakiTab.setContent(new StackPane());

        tabPane.getTabs().add(commonTab);
        tabPane.getTabs().add(mimakiTab);
        root.setCenter(tabPane);

        Scene scene = new Scene(root, 640, 480);
        stage.setScene(scene);
        stage.setTitle("ICC Profile Editor");
        stage.show();
    }

    private void openFile(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open ICC Profile");
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            filePathLabel.setText(file.getAbsolutePath());
        }
    }

    public static void main(String[] args) {
        launch();
    }

}
package com.mik.icc.icceditor;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

public class App extends Application {

    private TextArea headerTextArea = new TextArea("No file opened");
    private TableView<Tag> tagTableView = new TableView<>();
    private TextArea tagDataTextArea = new TextArea();
    private Button editButton = new Button("Edit");
    private Button saveButton = new Button("Save");
    private ICCProfile iccProfile;
    private Stage stage;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
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

        // Common Tab
        Tab commonTab = new Tab("Common");
        BorderPane commonTabPane = new BorderPane();
        SplitPane commonSplitPane = new SplitPane();
        commonSplitPane.setOrientation(javafx.geometry.Orientation.VERTICAL);
        SplitPane tagSplitPane = new SplitPane();
        tagSplitPane.getItems().addAll(tagTableView, tagDataTextArea);
        commonSplitPane.getItems().addAll(headerTextArea, tagSplitPane);
        commonTabPane.setCenter(commonSplitPane);

        HBox buttonBox = new HBox(10, editButton, saveButton);
        buttonBox.setPadding(new Insets(10));
        commonTabPane.setBottom(buttonBox);
        commonTab.setContent(commonTabPane);


        // Tag Table Columns
        TableColumn<Tag, String> signatureCol = new TableColumn<>("Signature");
        signatureCol.setCellValueFactory(new PropertyValueFactory<>("signature"));
        TableColumn<Tag, Long> offsetCol = new TableColumn<>("Offset");
        offsetCol.setCellValueFactory(new PropertyValueFactory<>("offset"));
        TableColumn<Tag, Long> sizeCol = new TableColumn<>("Size");
        sizeCol.setCellValueFactory(new PropertyValueFactory<>("size"));
        tagTableView.getColumns().addAll(signatureCol, offsetCol, sizeCol);

        tagTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                displayTagData(newSelection);
            }
        });

        // Buttons
        editButton.setOnAction(e -> {
            tagDataTextArea.setEditable(true);
            saveButton.setDisable(false);
            editButton.setDisable(true);
        });

        saveButton.setOnAction(e -> {
            tagDataTextArea.setEditable(false);
            saveButton.setDisable(true);
            editButton.setDisable(false);
        });
        saveButton.setDisable(true);
        tagDataTextArea.setEditable(false);

        Tab mimakiTab = new Tab("Mimaki");
        mimakiTab.setContent(new BorderPane());

        tabPane.getTabs().addAll(commonTab, mimakiTab);
        root.setCenter(tabPane);

        Scene scene = new Scene(root, 800, 600);
        stage.setScene(scene);
        stage.setTitle("ICC Profile Editor");
        stage.show();
    }

    private void openFile(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open ICC Profile");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("ICC Profiles", "*.icc", "*.icm"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            try {
                iccProfile = new ICCProfile(file.getAbsolutePath());
                headerTextArea.setText(iccProfile.getHeader().toString());
                ObservableList<Tag> tags = FXCollections.observableArrayList(iccProfile.getTags());
                tagTableView.setItems(tags);
                this.stage.setTitle("ICC Profile Editor - " + file.getName());
            } catch (IOException e) {
                headerTextArea.setText("Error reading ICC profile: " + e.getMessage());
            }
        }
    }

    private void displayTagData(Tag tag) {
        try {
            byte[] tagData = iccProfile.readTagData(tag);
            String text = bytesToHex(tagData) + "\n\n" + new String(tagData);
            if (tagData.length < tag.getSize()) {
                text += "\n\n... (data truncated)";
            }
            tagDataTextArea.setText(text);
        } catch (IOException e) {
            tagDataTextArea.setText("Error reading tag data: " + e.getMessage());
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        launch();
    }

}

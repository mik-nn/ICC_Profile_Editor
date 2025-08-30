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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

public class App extends Application {

    private TableView<Tag> tagTableView = new TableView<>();
    private TextArea tagDataTextArea = new TextArea();
    private Button editButton = new Button("Edit");
    private Button saveButton = new Button("Save");
    private ICCProfile iccProfile;
    private Stage stage;
    private GridPane headerEditor;
    private SplitPane commonSplitPane;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        BorderPane root = new BorderPane();

        // Menu
        MenuBar menuBar = new MenuBar();
        Menu fileMenu = new Menu("File");
        MenuItem openMenuItem = new MenuItem("Open");
        openMenuItem.setOnAction(e -> openFile(stage));
        MenuItem copyMenuItem = new MenuItem("Copy from another profile");
        copyMenuItem.setOnAction(e -> copyProfile(stage));
        fileMenu.getItems().addAll(openMenuItem, copyMenuItem);
        menuBar.getMenus().add(fileMenu);
        root.setTop(menuBar);

        // Tabs
        TabPane tabPane = new TabPane();

        // Common Tab
        Tab commonTab = new Tab("Common");
        BorderPane commonTabPane = new BorderPane();
        commonSplitPane = new SplitPane();
        commonSplitPane.setOrientation(javafx.geometry.Orientation.VERTICAL);
        SplitPane tagSplitPane = new SplitPane();
        tagSplitPane.getItems().addAll(tagTableView, tagDataTextArea);
        
        // Initialize headerEditor with a placeholder or empty data
        headerEditor = createHeaderEditor(new ICCHeader()); // Placeholder
        commonSplitPane.getItems().addAll(headerEditor, tagSplitPane);
        commonTabPane.setCenter(commonSplitPane);

        HBox buttonBox = new HBox(10, editButton, saveButton);
        buttonBox.setPadding(new Insets(10));
        commonTabPane.setBottom(buttonBox);
        commonTab.setContent(commonTabPane);

        // Add controls for hex/text mode and encoding
        HBox tagDataControls = new HBox(10);
        ToggleButton hexTextToggle = new ToggleButton("Hex/Text");
        ChoiceBox<String> encodingChoiceBox = new ChoiceBox<>();
        encodingChoiceBox.getItems().addAll("UTF-8", "US-ASCII", "UTF-16BE", "UTF-16LE");
        encodingChoiceBox.setValue("UTF-8"); // Default encoding
        tagDataControls.getChildren().addAll(hexTextToggle, encodingChoiceBox);
        commonTabPane.setTop(tagDataControls);


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
                // Reset toggle and encoding when a new tag is selected
                hexTextToggle.setSelected(false);
                encodingChoiceBox.setValue("UTF-8");
            }
        });

        hexTextToggle.setOnAction(e -> {
            Tag selectedTag = tagTableView.getSelectionModel().getSelectedItem();
            if (selectedTag != null) {
                try {
                    TagData currentTagData = iccProfile.readTagData(selectedTag);
                    if (hexTextToggle.isSelected()) { // Text mode
                        if (currentTagData instanceof TextTagData) {
                            tagDataTextArea.setText(((TextTagData) currentTagData).getText());
                        } else {
                            // Cannot display non-text data as text, revert to hex
                            tagDataTextArea.setText(bytesToHex(currentTagData.toBytes()));
                            hexTextToggle.setSelected(false);
                        }
                    } else { // Hex mode
                        tagDataTextArea.setText(bytesToHex(currentTagData.toBytes()));
                    }
                } catch (IOException ex) {
                    tagDataTextArea.setText("Error reading tag data: " + ex.getMessage());
                }
            }
        });

        encodingChoiceBox.setOnAction(e -> {
            Tag selectedTag = tagTableView.getSelectionModel().getSelectedItem();
            if (selectedTag != null && hexTextToggle.isSelected()) { // Only re-interpret if in text mode
                try {
                    TagData currentTagData = iccProfile.readTagData(selectedTag);
                    if (currentTagData instanceof TextTagData) {
                        Charset selectedCharset = Charset.forName(encodingChoiceBox.getValue());
                        String reinterpretedText = new String(currentTagData.toBytes(), selectedCharset);
                        tagDataTextArea.setText(reinterpretedText);
                    }
                } catch (IOException ex) {
                    tagDataTextArea.setText("Error re-interpreting tag data: " + ex.getMessage());
                }
            }
        });

        // Buttons
        editButton.setOnAction(e -> {
            tagDataTextArea.setEditable(true);
            saveButton.setDisable(false);
            editButton.setDisable(true);
            // Disable hex/text toggle and encoding choice while editing
            hexTextToggle.setDisable(true);
            encodingChoiceBox.setDisable(true);
        });

        saveButton.setOnAction(e -> {
            tagDataTextArea.setEditable(false);
            saveButton.setDisable(true);
            editButton.setDisable(false);
            hexTextToggle.setDisable(false);
            encodingChoiceBox.setDisable(false);

            Tag selectedTag = tagTableView.getSelectionModel().getSelectedItem();
            if (selectedTag != null) {
                try {
                    TagData newTagData;
                    if (hexTextToggle.isSelected()) { // Text mode
                        Charset selectedCharset = Charset.forName(encodingChoiceBox.getValue());
                        newTagData = new TextTagData(tagDataTextArea.getText(), selectedCharset);
                    } else { // Hex mode
                        String hexString = tagDataTextArea.getText().replaceAll("\\s+", "");
                        byte[] data = new byte[hexString.length() / 2];
                        for (int i = 0; i < hexString.length(); i += 2) {
                            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                                                 + Character.digit(hexString.charAt(i + 1), 16));
                        }
                        newTagData = new GenericTagData(data);
                    }
                    iccProfile.writeTagData(selectedTag, newTagData);
                } catch (IOException | NumberFormatException ex) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Error saving tag data");
                    alert.setContentText(ex.getMessage());
                    alert.showAndWait();
                }
            }
        });
        saveButton.setDisable(true);
        tagDataTextArea.setEditable(false);

        Tab mimakiTab = new Tab("Mimaki");
        BorderPane mimakiTabPane = new BorderPane();
        VBox mimakiControls = new VBox(10);
        mimakiControls.setPadding(new Insets(10));

        // Media Name Search/Replace
        Label mediaNameLabel = new Label("Media Name Search/Replace");
        TextField searchField = new TextField();
        searchField.setPromptText("Search for...");
        TextField replaceField = new TextField();
        replaceField.setPromptText("Replace with...");
        Button searchReplaceButton = new Button("Search and Replace");

        searchReplaceButton.setOnAction(e -> {
            if (iccProfile == null) {
                showAlert(Alert.AlertType.WARNING, "No Profile", "Please open an ICC profile first.");
                return;
            }

            String searchText = searchField.getText();
            String replaceText = replaceField.getText();

            if (searchText.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Input Missing", "Please enter text to search for.");
                return;
            }

            try {
                Tag descTag = iccProfile.getTagBySignature("desc");
                if (descTag != null) {
                    TagData currentDescData = iccProfile.readTagData(descTag);
                    if (currentDescData instanceof TextTagData) {
                        TextTagData textDescData = (TextTagData) currentDescData;
                        String originalText = textDescData.getText();
                        String newText = originalText.replace(searchText, replaceText);
                        if (!originalText.equals(newText)) {
                            iccProfile.writeTagData(descTag, new TextTagData(newText, textDescData.getCharset()));
                            showAlert(Alert.AlertType.INFORMATION, "Success", "Media name replaced successfully.");
                            // Refresh the UI to show changes
                            displayTagData(descTag);
                        } else {
                            showAlert(Alert.AlertType.INFORMATION, "No Change", "Search text not found in media name.");
                        }
                    } else {
                        showAlert(Alert.AlertType.WARNING, "Unsupported Tag Type", "The 'desc' tag is not a text type.");
                    }
                } else {
                    showAlert(Alert.AlertType.WARNING, "Tag Not Found", "'desc' tag not found in the profile.");
                }
            } catch (IOException ex) {
                showAlert(Alert.AlertType.ERROR, "Error", "Error during search and replace: " + ex.getMessage());
            }
        });

        mimakiControls.getChildren().addAll(mediaNameLabel, searchField, replaceField, searchReplaceButton);

        // Cxf->DevS/CIED Conversion
        Label conversionLabel = new Label("Cxf -> DevS/CIED Conversion");
        Button convertButton = new Button("Convert");
        convertButton.setOnAction(e -> {
            showAlert(Alert.AlertType.INFORMATION, "Under Construction", "Cxf -> DevS/CIED Conversion is not yet implemented.");
        });

        mimakiControls.getChildren().addAll(conversionLabel, convertButton);

        mimakiTabPane.setCenter(mimakiControls);
        mimakiTab.setContent(mimakiTabPane);

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
                // Update the header editor with the new profile's header
                commonSplitPane.getItems().remove(headerEditor);
                headerEditor = createHeaderEditor(iccProfile.getHeader());
                commonSplitPane.getItems().add(0, headerEditor);

                ObservableList<Tag> tags = FXCollections.observableArrayList(iccProfile.getTags());
                tagTableView.setItems(tags);
                this.stage.setTitle("ICC Profile Editor - " + file.getName());
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Error reading ICC profile: " + e.getMessage());
            }
        }
    }

    private void copyProfile(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Copy from another ICC Profile");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("ICC Profiles", "*.icc", "*.icm"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            try {
                ICCProfile sourceProfile = new ICCProfile(file.getAbsolutePath());
                // Copy tags from sourceProfile to iccProfile
                for (Tag sourceTag : sourceProfile.getTags()) {
                    Tag currentTag = iccProfile.getTagBySignature(sourceTag.getSignature());
                    if (currentTag != null) {
                        TagData sourceTagData = sourceProfile.readTagData(sourceTag);
                        iccProfile.writeTagData(currentTag, sourceTagData);
                    }
                }
                // Refresh UI
                commonSplitPane.getItems().remove(headerEditor);
                headerEditor = createHeaderEditor(iccProfile.getHeader());
                commonSplitPane.getItems().add(0, headerEditor);

                ObservableList<Tag> tags = FXCollections.observableArrayList(iccProfile.getTags());
                tagTableView.setItems(tags);
                this.stage.setTitle("ICC Profile Editor - (Copied) " + file.getName());
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Error copying ICC profile: " + e.getMessage());
            }
        }
    }

    private void displayTagData(Tag tag) {
        try {
            TagData tagData = iccProfile.readTagData(tag);
            if (tagData instanceof TextTagData) {
                tagDataTextArea.setText(((TextTagData) tagData).getText());
            } else if (tagData instanceof GenericTagData) {
                tagDataTextArea.setText(bytesToHex(tagData.toBytes()));
            } else {
                tagDataTextArea.setText("Unsupported Tag Data Type");
            }
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

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private GridPane createHeaderEditor(ICCHeader header) {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10));
        grid.setHgap(10);
        grid.setVgap(5);

        int row = 0;
        grid.addRow(row++, new Label("Size:"), new TextField(String.valueOf(header.size)));
        grid.addRow(row++, new Label("CMM Type:"), new TextField(header.cmmType));
        grid.addRow(row++, new Label("Version:"), new TextField(header.version));
        grid.addRow(row++, new Label("Device Class:"), new TextField(header.deviceClass));
        grid.addRow(row++, new Label("Color Space:"), new TextField(header.colorSpace));
        grid.addRow(row++, new Label("PCS:"), new TextField(header.pcs));
        grid.addRow(row++, new Label("Creation Date/Time:"), new TextField(header.creationDateTime));
        grid.addRow(row++, new Label("Signature:"), new TextField(header.signature));
        grid.addRow(row++, new Label("Primary Platform:"), new TextField(header.primaryPlatform));
        grid.addRow(row++, new Label("Flags:"), new TextField(String.valueOf(header.flags)));
        grid.addRow(row++, new Label("Manufacturer:"), new TextField(header.manufacturer));
        grid.addRow(row++, new Label("Model:"), new TextField(header.model));
        grid.addRow(row++, new Label("Attributes:"), new TextField(String.valueOf(header.attributes)));
        grid.addRow(row++, new Label("Rendering Intent:"), new TextField(header.getRenderingIntentString()));
        grid.addRow(row++, new Label("Creator:"), new TextField(header.creator));

        return grid;
    }

    public static void main(String[] args) {
        launch();
    }

}
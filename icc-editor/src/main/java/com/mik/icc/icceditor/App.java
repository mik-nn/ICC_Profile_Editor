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
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Consumer;
import javafx.beans.property.ReadOnlyObjectWrapper;

public class App extends Application {

    private TableView<Tag> tagTableView = new TableView<>();
    private TextArea tagDataTextArea = new TextArea();
    private StackPane tagEditorPane = new StackPane();
    private Button editButton = new Button("Edit");
    private Button saveButton = new Button("Save");
    private ICCProfile iccProfile;
    private Stage stage;
    private GridPane headerEditor;
    private SplitPane commonSplitPane;
    private ToggleButton hexTextToggle = new ToggleButton("Hex/Text");
    private ChoiceBox<String> encodingChoiceBox = new ChoiceBox<>();

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
        tagSplitPane.getItems().addAll(tagTableView, tagEditorPane);
        
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
            // For now, edit button primarily for MLUC or other complex editors
            // Simple text/hex/XYZ/Curve fields are double-click editable
            showAlert(Alert.AlertType.INFORMATION, "Edit Mode", "Double-click fields to edit them.");
        });

        saveButton.setOnAction(e -> {
            Tag selectedTag = tagTableView.getSelectionModel().getSelectedItem();
            if (selectedTag != null) {
                try {
                    TagData newTagData = null;
                    if (tagEditorPane.getChildren().get(0) instanceof TextArea) { // Text, Generic, or Curve
                        TextArea currentTextArea = (TextArea) tagEditorPane.getChildren().get(0);
                        if (TagType.fromSignature(selectedTag.getSignature()) == TagType.TEXT_TYPE) {
                            Charset selectedCharset = Charset.forName(encodingChoiceBox.getValue());
                            newTagData = new TextTagData(currentTextArea.getText(), selectedCharset);
                        } else if (TagType.fromSignature(selectedTag.getSignature()) == TagType.CURVE_TYPE) {
                            String[] points = currentTextArea.getText().replace("Curve Points: ", "").replace("[", "").replace("]", "").split(", ");
                            double[] curvePoints = new double[points.length];
                            for (int i = 0; i < points.length; i++) {
                                curvePoints[i] = Double.parseDouble(points[i]);
                            }
                            newTagData = new CurveTagData(curvePoints);
                        } else { // Generic
                            String hexString = currentTextArea.getText().replaceAll("\\s+", "");
                            byte[] data = new byte[hexString.length() / 2];
                            for (int i = 0; i < hexString.length(); i += 2) {
                                data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                                                     + Character.digit(hexString.charAt(i + 1), 16));
                            }
                            newTagData = new GenericTagData(data);
                        }
                    } else if (tagEditorPane.getChildren().get(0) instanceof GridPane) { // XYZ
                        GridPane xyzEditor = (GridPane) tagEditorPane.getChildren().get(0);
                        double x = Double.parseDouble(((TextField) xyzEditor.getChildren().get(1)).getText());
                        double y = Double.parseDouble(((TextField) xyzEditor.getChildren().get(3)).getText());
                        double z = Double.parseDouble(((TextField) xyzEditor.getChildren().get(5)).getText());
                        newTagData = new XYZTagData(x, y, z);
                    } else if (tagEditorPane.getChildren().get(0) instanceof TableView) { // MLUC
                        TableView<Map.Entry<String, String>> mlucTableView = (TableView) tagEditorPane.getChildren().get(0);
                        MultiLocalizedUnicodeTagData mlucData = new MultiLocalizedUnicodeTagData();
                        for (Map.Entry<String, String> entry : mlucTableView.getItems()) {
                            String[] codes = entry.getKey().split("-");
                            mlucData.addLocalizedString(codes[0], codes[1], entry.getValue());
                        }
                        newTagData = mlucData;
                    }

                    if (newTagData != null) {
                        iccProfile.writeTagData(selectedTag, newTagData);
                    }
                } catch (IOException | NumberFormatException ex) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Error saving tag data");
                    alert.setContentText(ex.getMessage());
                    alert.showAndWait();
                }
            }
        });
        saveButton.setDisable(false); // Always enable save button for now
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

            boolean replaced = false;

            // Search and replace in 'desc' tag
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
                            replaced = true;
                        }
                    } else {
                        showAlert(Alert.AlertType.WARNING, "Unsupported Tag Type", "The 'desc' tag is not a text type.");
                    }
                }
            } catch (IOException ex) {
                showAlert(Alert.AlertType.ERROR, "Error", "Error during search and replace in 'desc' tag: " + ex.getMessage());
            }

            // Search and replace in 'MMK1' and 'MMK2' tags (experimental)
            String[] mimakiTags = {"MMK1", "MMK2"};
            for (String mimakiTagSignature : mimakiTags) {
                try {
                    Tag mimakiTag = iccProfile.getTagBySignature(mimakiTagSignature);
                    if (mimakiTag != null) {
                        TagData currentMimakiData = iccProfile.readTagData(mimakiTag);
                        // Attempt to interpret as UTF-8 string
                        String originalText = new String(currentMimakiData.toBytes(), StandardCharsets.UTF_8);
                        String newText = originalText.replace(searchText, replaceText);
                        if (!originalText.equals(newText)) {
                            iccProfile.writeTagData(mimakiTag, new GenericTagData(newText.getBytes(StandardCharsets.UTF_8)));
                            replaced = true;
                        }
                    }
                } catch (IOException ex) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Error during search and replace in '" + mimakiTagSignature + "' tag: " + ex.getMessage());
                }
            }

            if (replaced) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Media name replaced successfully. (Experimental for MMK tags)");
                // Refresh the UI to show changes
                // Re-open the file to refresh all data
                try {
                    iccProfile = new ICCProfile(iccProfile.getFilePath()); // Re-read the profile
                    commonSplitPane.getItems().remove(headerEditor);
                    headerEditor = createHeaderEditor(iccProfile.getHeader());
                    commonSplitPane.getItems().add(0, headerEditor);
                    ObservableList<Tag> tags = FXCollections.observableArrayList(iccProfile.getTags());
                    tagTableView.setItems(tags);
                } catch (IOException ex) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Error refreshing profile after save: " + ex.getMessage());
                }
            } else {
                showAlert(Alert.AlertType.INFORMATION, "No Change", "Search text not found in any relevant media name tag.");
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
        scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
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
        tagEditorPane.getChildren().clear(); // Clear previous editor
        try {
            TagData tagData = iccProfile.readTagData(tag);
            boolean isTextOrGeneric = (tagData instanceof TextTagData || tagData instanceof GenericTagData);
            hexTextToggle.setDisable(!isTextOrGeneric);
            encodingChoiceBox.setDisable(!isTextOrGeneric);

            if (tagData instanceof TextTagData) {
                tagDataTextArea.setText(((TextTagData) tagData).getText());
                tagDataTextArea.setEditable(false); // Default to non-editable
                tagDataTextArea.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2) {
                        tagDataTextArea.setEditable(true);
                    }
                });
                tagDataTextArea.focusedProperty().addListener((obs, oldVal, newVal) -> {
                    if (!newVal) {
                        tagDataTextArea.setEditable(false);
                        // Save logic will be handled by saveButton
                    }
                });
                tagDataTextArea.setOnAction(event -> {
                    tagDataTextArea.setEditable(false);
                    // Save logic will be handled by saveButton
                });
                tagEditorPane.getChildren().add(tagDataTextArea);
            } else if (tagData instanceof XYZTagData) {
                XYZTagData xyzData = (XYZTagData) tagData;
                GridPane xyzEditor = new GridPane();
                xyzEditor.setPadding(new Insets(10));
                xyzEditor.setHgap(10);
                xyzEditor.setVgap(5);
                xyzEditor.addRow(0, new Label("X:"), createEditableXYZTextField(String.valueOf(xyzData.getX()), xyzData, "x"));
                xyzEditor.addRow(1, new Label("Y:"), createEditableXYZTextField(String.valueOf(xyzData.getY()), xyzData, "y"));
                xyzEditor.addRow(2, new Label("Z:"), createEditableXYZTextField(String.valueOf(xyzData.getZ()), xyzData, "z"));
                tagEditorPane.getChildren().add(xyzEditor);
            } else if (tagData instanceof CurveTagData) {
                CurveTagData curveData = (CurveTagData) tagData;
                TextArea curveTextArea = new TextArea(curveData.toString());
                curveTextArea.setEditable(false);
                curveTextArea.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2) {
                        curveTextArea.setEditable(true);
                    }
                });
                curveTextArea.focusedProperty().addListener((obs, oldVal, newVal) -> {
                    if (!newVal) {
                        curveTextArea.setEditable(false);
                        // Save logic will be handled by saveButton
                    }
                });
                curveTextArea.setOnAction(event -> {
                    curveTextArea.setEditable(false);
                    // Save logic will be handled by saveButton
                });
                tagEditorPane.getChildren().add(curveTextArea);
            } else if (tagData instanceof MultiLocalizedUnicodeTagData) {
                MultiLocalizedUnicodeTagData mlucData = (MultiLocalizedUnicodeTagData) tagData;
                TableView<Map.Entry<String, String>> mlucTableView = new TableView<>();
                TableColumn<Map.Entry<String, String>, String> langCountryCol = new TableColumn<>("Language-Country");
                langCountryCol.setCellValueFactory(p -> new ReadOnlyObjectWrapper<>(p.getValue().getKey()));
                TableColumn<Map.Entry<String, String>, String> textCol = new TableColumn<>("Text");
                textCol.setCellValueFactory(p -> new ReadOnlyObjectWrapper<>(p.getValue().getValue()));
                mlucTableView.getColumns().addAll(langCountryCol, textCol);
                mlucTableView.setItems(FXCollections.observableArrayList(mlucData.getLocalizedStrings().entrySet()));
                tagEditorPane.getChildren().add(mlucTableView);
            } else if (tagData instanceof GenericTagData) {
                tagDataTextArea.setText(bytesToHex(tagData.toBytes()));
                tagDataTextArea.setEditable(false); // Default to non-editable
                tagDataTextArea.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2) {
                        tagDataTextArea.setEditable(true);
                    }
                });
                tagDataTextArea.focusedProperty().addListener((obs, oldVal, newVal) -> {
                    if (!newVal) {
                        tagDataTextArea.setEditable(false);
                        // Save logic will be handled by saveButton
                    }
                });
                tagDataTextArea.setOnAction(event -> {
                    tagDataTextArea.setEditable(false);
                    // Save logic will be handled by saveButton
                });
                tagEditorPane.getChildren().add(tagDataTextArea);
            } else {
                tagDataTextArea.setText("Unsupported Tag Data Type");
                tagEditorPane.getChildren().add(tagDataTextArea);
            }
        } catch (IOException e) {
            tagDataTextArea.setText("Error reading tag data: " + e.getMessage());
            tagEditorPane.getChildren().add(tagDataTextArea);
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
        grid.addRow(row++, new Label("Size:"), createEditableTextField(String.valueOf(header.size), newValue -> header.size = Long.parseLong(newValue)));
        grid.addRow(row++, new Label("CMM Type:"), createEditableTextField(header.cmmType, newValue -> header.cmmType = newValue));
        grid.addRow(row++, new Label("Version:"), createEditableTextField(header.version, newValue -> header.version = newValue));
        grid.addRow(row++, new Label("Device Class:"), createEditableTextField(header.deviceClass, newValue -> header.deviceClass = newValue));
        grid.addRow(row++, new Label("Color Space:"), createEditableTextField(header.colorSpace, newValue -> header.colorSpace = newValue));
        grid.addRow(row++, new Label("PCS:"), createEditableTextField(header.pcs, newValue -> header.pcs = newValue));
        grid.addRow(row++, new Label("Creation Date/Time:"), createEditableTextField(header.creationDateTime, newValue -> header.creationDateTime = newValue));
        grid.addRow(row++, new Label("Signature:"), createEditableTextField(header.signature, newValue -> header.signature = newValue));
        grid.addRow(row++, new Label("Primary Platform:"), createEditableTextField(header.primaryPlatform, newValue -> header.primaryPlatform = newValue));
        grid.addRow(row++, new Label("Flags:"), createEditableTextField(String.valueOf(header.flags), newValue -> header.flags = Long.parseLong(newValue)));
        grid.addRow(row++, new Label("Manufacturer:"), createEditableTextField(header.manufacturer, newValue -> header.manufacturer = newValue));
        grid.addRow(row++, new Label("Model:"), createEditableTextField(header.model, newValue -> header.model = newValue));
        grid.addRow(row++, new Label("Attributes:"), createEditableTextField(String.valueOf(header.attributes), newValue -> header.attributes = Long.parseLong(newValue)));
        grid.addRow(row++, new Label("Rendering Intent:"), createEditableTextField(header.getRenderingIntentString(), newValue -> header.renderingIntent = parseRenderingIntent(newValue)));
        grid.addRow(row++, new Label("Creator:"), createEditableTextField(header.creator, newValue -> header.creator = newValue));

        return grid;
    }

    private TextField createEditableTextField(String initialValue, Consumer<String> onSave) {
        TextField textField = new TextField(initialValue);
        textField.setEditable(false);
        textField.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                textField.setEditable(true);
            }
        });
        textField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                textField.setEditable(false);
                onSave.accept(textField.getText());
                try {
                    iccProfile.writeHeader(iccProfile.getHeader());
                } catch (IOException e) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Error writing header: " + e.getMessage());
                }
            }
        });
        textField.setOnAction(event -> {
            textField.setEditable(false);
            onSave.accept(textField.getText());
        });
        return textField;
    }

    private int parseRenderingIntent(String intent) {
        return switch (intent) {
            case "Perceptual" -> 0;
            case "Relative Colorimetric" -> 1;
            case "Saturation" -> 2;
            case "Absolute Colorimetric" -> 3;
            default -> -1; // Indicate unknown or error
        };
    }

    private TextField createEditableXYZTextField(String initialValue, XYZTagData xyzData, String field) {
        TextField textField = new TextField(initialValue);
        textField.setEditable(false);
        textField.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                textField.setEditable(true);
            }
        });
        textField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                textField.setEditable(false);
                try {
                    double value = Double.parseDouble(textField.getText());
                    if (field.equals("x")) {
                        xyzData.setX(value);
                    } else if (field.equals("y")) {
                        xyzData.setY(value);
                    } else if (field.equals("z")) {
                        xyzData.setZ(value);
                    }
                    // Save logic will be handled by saveButton
                } catch (NumberFormatException e) {
                    showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter a valid number.");
                }
            }
        });
        textField.setOnAction(event -> {
            textField.setEditable(false);
            try {
                double value = Double.parseDouble(textField.getText());
                if (field.equals("x")) {
                    // xyzData.setX(value); // Need setter in XYZTagData
                } else if (field.equals("y")) {
                    // xyzData.setY(value); // Need setter in XYZTagData
                } else if (field.equals("z")) {
                    // xyzData.setZ(value); // Need setter in XYZTagData
                }
                // Save logic will be handled by saveButton
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter a valid number.");
            }
        });
        return textField;
    }

    public static void main(String[] args) {
        launch();
    }

}
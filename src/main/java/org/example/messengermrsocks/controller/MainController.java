package org.example.messengermrsocks.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class MainController {

    @FXML
    private SplitPane splitPane;

    @FXML
    private ListView<?> ListViewDialog;

    @FXML
    private ListView<?> listViewHistoryChat;

    @FXML
    private TextArea sendTextField;


    public void initialize() {
        double minPosition = 0.2; // Минимальное положение делителя (20% от начала)
        double maxPosition = 0.7; // Максимальное положение делителя (70% от начала)
        double currentPosition = 0.3;
        // Устанавливаем слушатель изменений положения делителя
        splitPane.getDividers().get(0).positionProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() < minPosition) {
                splitPane.setDividerPositions(minPosition);
            } else if (newVal.doubleValue() > maxPosition) {
                splitPane.setDividerPositions(maxPosition);
            }
        });

        sendTextField.setOnKeyPressed(event -> handleKeyPress(event, sendTextField));
    }

    private void handleKeyPress(KeyEvent event, TextArea textArea) {
        if (event.getCode() == KeyCode.ENTER && !event.isShiftDown()) {

            // Действие, которое вы хотите выполнить при нажатии Enter
            System.out.print("Enter pressed! Current text: " + textArea.getText());
            sendTextField.clear();
            event.consume();
             // Предотвращаем добавление новой строки по умолчанию
        }
        if (event.getCode() == KeyCode.ENTER && event.isShiftDown()) {
            // Добавляем новую строку при Shift + Enter
            textArea.appendText("\n");
            event.consume(); // Предотвращаем стандартное поведение Enter
        }
    }

    private void viewHistoryChat(){

    }


}

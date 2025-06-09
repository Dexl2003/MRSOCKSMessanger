package org.example.messengermrsocks.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.example.messengermrsocks.model.Messages.Message;
import java.io.File;

public class MessageListCellController {
    @FXML public HBox rootHBox;
    @FXML public ImageView avatarImageView;
    @FXML public Label messageTextLabel;
    @FXML public Label timeLabel;
    @FXML public Label sentCheckmark;
    @FXML public Label receivedCheckmark;
    @FXML public ImageView mediaPreviewImageView;
    @FXML public VBox bubbleVBox;
    @FXML public HBox timeStatusHBox;

    public void updateMessageStatus(Message message) {
        // --- Скрываем текст, если только изображение ---
        boolean hasImage = false;
        if (message.getMediaFilePath() != null) {
            File file = new File(message.getMediaFilePath());
            hasImage = file.exists() && isImageFile(file.getName());
        }
        boolean showText = message.getText() != null && !message.getText().isEmpty();
        if (!showText && hasImage) {
            messageTextLabel.setVisible(false);
            messageTextLabel.setManaged(false);
        } else {
            messageTextLabel.setVisible(true);
            messageTextLabel.setManaged(true);
        }

        // --- Ограничение максимальной ширины пузыря ---
        double maxWidth = 400; // fallback
        try {
            if (MainController.listViewHistoryChatStatic != null) {
                maxWidth = MainController.listViewHistoryChatStatic.getWidth() * 0.6;
            }
        } catch (Exception ignored) {}
        bubbleVBox.setMaxWidth(maxWidth);

        // --- Цвет пузыря ---
        if (message.isOwn()) {
            sentCheckmark.setVisible(message.isSent());
            receivedCheckmark.setVisible(message.isReceived());
            if (message.isSent()) {
                sentCheckmark.setStyle("-fx-text-fill: #90EE90; -fx-font-size: 12;");
            }
            if (message.isReceived()) {
                receivedCheckmark.setStyle("-fx-text-fill: #90EE90; -fx-font-size: 12;");
            }
            bubbleVBox.setStyle("-fx-background-color: #dcf8c6; -fx-background-radius: 8; -fx-padding: 4;");
        } else {
            sentCheckmark.setVisible(false);
            receivedCheckmark.setVisible(false);
            bubbleVBox.setStyle("-fx-background-color: #fff; -fx-background-radius: 8; -fx-padding: 4;");
        }

        // --- Предпросмотр медиа ---
        if (mediaPreviewImageView != null) {
            mediaPreviewImageView.setVisible(false);
            mediaPreviewImageView.setManaged(false);
        }
        if (message.getMediaFilePath() != null) {
            File file = new File(message.getMediaFilePath());
            if (file.exists() && isImageFile(file.getName())) {
                if (mediaPreviewImageView != null) {
                    double fitWidth = 200; // fallback
                    try {
                        if (MainController.listViewHistoryChatStatic != null) {
                            fitWidth = MainController.listViewHistoryChatStatic.getWidth() * 0.5;
                        }
                    } catch (Exception ignored) {}
                    mediaPreviewImageView.setFitWidth(fitWidth);
                    mediaPreviewImageView.setImage(new Image(file.toURI().toString(), fitWidth, 0, true, true));
                    mediaPreviewImageView.setVisible(true);
                    mediaPreviewImageView.setManaged(true);
                }
            }
        }
        // --- Время и статус всегда видимы ---
        if (timeStatusHBox != null) {
            timeStatusHBox.setVisible(true);
            timeStatusHBox.setManaged(true);
        }
    }

    private boolean isImageFile(String fileName) {
        String lower = fileName.toLowerCase();
        return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".gif") || lower.endsWith(".bmp");
    }
} 
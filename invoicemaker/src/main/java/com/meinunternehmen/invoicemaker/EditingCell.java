package com.meinunternehmen.invoicemaker;

import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

public class EditingCell<T> extends TableCell<InvoiceLine,T> {
    private final TextField textField = new TextField();
    private final StringConverter<T> converter;

    public EditingCell(StringConverter<T> converter) {
        this.converter = converter;

        textField.setOnAction(evt -> {
            commitEdit(converter.fromString(textField.getText()));
        });
        textField.focusedProperty().addListener((ignored, notused, isNowFoc) -> {
            if (!isNowFoc) {
                commitEdit(converter.fromString(textField.getText()));
            }
        });
    }

    @Override
    public void startEdit() {
        super.startEdit();
        T item = getItem();
        textField.setText(item == null ? "" : converter.toString(item));
        setGraphic(textField);
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        textField.requestFocus();
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        T item = getItem();
        setText(item == null ? "" : converter.toString(item));
        setGraphic(null);
        setContentDisplay(ContentDisplay.TEXT_ONLY);
    }

    @Override
    protected void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);

        if (empty) {
            setText(null);
            setGraphic(null);

        } else if (isEditing()) {
            setText(null);
            setGraphic(textField);
            textField.setText(item == null ? "" : converter.toString(item));

        } else {
            setText(item == null ? "" : converter.toString(item));
            setGraphic(null);
        }
    }
}

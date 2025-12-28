package com.example.phonedir;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

class MaterialAddDialog extends JDialog {
    interface OnSave { void accept(Contact c); }

    private final JTextField name = new JTextField(22);
    private final JTextField phone = new JTextField(22);
    private final JTextField email = new JTextField(22);
    private final JLabel error = new JLabel(" ");
    private final JButton saveButton = new JButton("Save");

    MaterialAddDialog(Window owner, OnSave onSave) {
        super(owner, "Add Contact", ModalityType.APPLICATION_MODAL);
        ((JComponent) getContentPane()).setBorder(Style.padding(16,16,16,16));
        setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(8,8,8,8);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridx=0; gc.gridy=0; add(new JLabel("Name"), gc); gc.gridx=1; gc.weightx=1; add(name, gc);
        gc.gridx=0; gc.gridy=1; gc.weightx=0; add(new JLabel("Phone Number"), gc); gc.gridx=1; gc.weightx=1; add(phone, gc);
        gc.gridx=0; gc.gridy=2; gc.weightx=0; add(new JLabel("Email"), gc); gc.gridx=1; gc.weightx=1; add(email, gc);

        // Inline validation message
        gc.gridx=0; gc.gridy=3; gc.gridwidth=2; gc.weightx=1; gc.fill=GridBagConstraints.HORIZONTAL;
        error.setForeground(new Color(0xE05555));
        error.setFont(error.getFont().deriveFont(Font.PLAIN, 11f));
        add(error, gc);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancel = new JButton("Cancel");
        Style.softenButtons(saveButton, cancel);
        actions.add(cancel); actions.add(saveButton);
        gc.gridx=0; gc.gridy=4; gc.gridwidth=2; gc.weightx=0; gc.fill=GridBagConstraints.NONE; add(actions, gc);

        cancel.addActionListener(e -> dispose());
        saveButton.setBackground(new Color(0x16C1B5));
        saveButton.setForeground(Color.WHITE);
        saveButton.addActionListener(e -> {
            validateForm();
            String n = name.getText().trim();
            if (n.isEmpty()) {
                return;
            }
            String ph = phone.getText().trim(); if (ph.isEmpty()) ph = null;
            String em = email.getText().trim(); if (em.isEmpty()) em = null;
            onSave.accept(new Contact(n, ph, em));
            dispose();
        });

        // live validation on name/email
        DocumentListener dl = new DocumentListener() {
            private void changed() { validateForm(); }
            public void insertUpdate(DocumentEvent e) { changed(); }
            public void removeUpdate(DocumentEvent e) { changed(); }
            public void changedUpdate(DocumentEvent e) { changed(); }
        };
        name.getDocument().addDocumentListener(dl);
        email.getDocument().addDocumentListener(dl);

        validateForm();
        pack();
        setLocationRelativeTo(owner);
    }

    void setInitial(Contact c) {
        if (c == null) return;
        name.setText(c.getName());
        phone.setText(c.getPhoneNumber());
        email.setText(c.getEmail());
        validateForm();
    }

    private void validateForm() {
        String n = name.getText().trim();
        String em = email.getText().trim();
        if (n.isEmpty()) {
            error.setText("Name is required");
            saveButton.setEnabled(false);
            return;
        }
        // very light email hint, does not block saving
        if (!em.isEmpty() && !em.contains("@")) {
            error.setText("Email looks unusual");
        } else {
            error.setText(" ");
        }
        saveButton.setEnabled(true);
    }
}

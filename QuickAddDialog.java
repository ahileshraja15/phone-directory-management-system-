package com.example.phonedir;

import javax.swing.*;
import java.awt.*;

public class QuickAddDialog extends JDialog {
    public interface OnCreated { void created(Contact contact); }

    private final PhoneDirectory directory;
    private final JTextField name = new JTextField(20);
    private final JTextField phone = new JTextField(16);
    private final JTextField email = new JTextField(20);

    public QuickAddDialog(JFrame owner, PhoneDirectory directory, OnCreated callback) {
        super(owner, "Quick Add Contact", true);
        this.directory = directory;
        setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6,6,6,6);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridx = 0; gc.gridy = 0; add(new JLabel("Name"), gc); gc.gridx = 1; gc.weightx = 1; add(name, gc);
        gc.gridx = 0; gc.gridy = 1; gc.weightx = 0; add(new JLabel("Phone"), gc); gc.gridx = 1; gc.weightx = 1; add(phone, gc);
        gc.gridx = 0; gc.gridy = 2; gc.weightx = 0; add(new JLabel("Email"), gc); gc.gridx = 1; gc.weightx = 1; add(email, gc);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancel = new JButton("Cancel");
        JButton save = new JButton("Save");
        ((JComponent) getContentPane()).setBorder(Style.padding(12,12,12,12));
        Style.softenButtons(cancel, save);
        actions.add(cancel); actions.add(save);
        gc.gridx = 0; gc.gridy = 3; gc.gridwidth = 2; gc.weightx = 0; add(actions, gc);

        cancel.addActionListener(e -> dispose());
        save.addActionListener(e -> {
            String n = name.getText().trim();
            if (n.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Name is required", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String ph = phone.getText().trim(); if (ph.isEmpty()) ph = null;
            String em = email.getText().trim(); if (em.isEmpty()) em = null;
            try {
                Contact c = new Contact(n, ph, em);
                directory.addContact(c);
                if (callback != null) callback.created(c);
                dispose();
            } catch (DuplicateContactException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Duplicate", JOptionPane.WARNING_MESSAGE);
            }
        });

        pack();
        setLocationRelativeTo(owner);
    }
}
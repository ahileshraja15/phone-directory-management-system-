package com.example.phonedir;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PhoneDirectoryUI extends JFrame {
    private final PhoneDirectory directory;
    private final File dataFile;

    private final JTextField searchField = new JTextField(20);
    private final JTable table = new JTable();
    private final ContactsTableModel tableModel = new ContactsTableModel();

    private final JTextField nameField = new JTextField(16);
    private final JTextField phoneField = new JTextField(12);
    private final JTextField emailField = new JTextField(16);

    public PhoneDirectoryUI(PhoneDirectory directory, File dataFile) {
        super("Phone Directory");
        this.directory = directory;
        this.dataFile = dataFile;

        applyTheme();
        buildUI();
        refreshTable("");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(760, 520);
    }

    private void applyTheme() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) { }
        // Subtle Nimbus tweaks
        UIManager.put("control", new Color(245, 246, 248));
        UIManager.put("nimbusBlueGrey", new Color(120, 130, 140));
        UIManager.put("nimbusFocus", new Color(76, 132, 255));
        UIManager.put("text", new Color(25, 25, 28));
    }

    private void buildUI() {
        setLayout(new BorderLayout(8, 8));

        // Top: search
        JPanel top = new JPanel(new BorderLayout(8, 8));
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);
        JButton searchBtn = new JButton(new AbstractAction("Search") {
            @Override public void actionPerformed(ActionEvent e) { refreshTable(searchField.getText()); }
        });
        searchPanel.add(searchBtn);
        top.add(searchPanel, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        // Center: table
        table.setModel(tableModel);
        table.setFillsViewportHeight(true);
        JScrollPane scroll = new JScrollPane(table);
        add(scroll, BorderLayout.CENTER);

        // Bottom: add/remove form
        JPanel form = new JPanel();
        form.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 4, 4, 4);
        gc.gridy = 0; gc.gridx = 0; form.add(new JLabel("Name"), gc);
        gc.gridx = 1; form.add(nameField, gc);
        gc.gridy = 1; gc.gridx = 0; form.add(new JLabel("Phone"), gc);
        gc.gridx = 1; form.add(phoneField, gc);
        gc.gridy = 2; gc.gridx = 0; form.add(new JLabel("Email"), gc);
        gc.gridx = 1; form.add(emailField, gc);

        JButton addBtn = new JButton(new AbstractAction("Add / Update") {
            @Override public void actionPerformed(ActionEvent e) { onAddOrUpdate(); }
        });
        JButton deleteBtn = new JButton(new AbstractAction("Delete Selected") {
            @Override public void actionPerformed(ActionEvent e) { onDeleteSelected(); }
        });
        JButton saveBtn = new JButton(new AbstractAction("Save") {
            @Override public void actionPerformed(ActionEvent e) { onSave(); }
        });

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(addBtn);
        actions.add(deleteBtn);
        actions.add(saveBtn);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(form, BorderLayout.CENTER);
        bottom.add(actions, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);

        // Live search
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refreshTable(searchField.getText()); }
            public void removeUpdate(DocumentEvent e) { refreshTable(searchField.getText()); }
            public void changedUpdate(DocumentEvent e) { refreshTable(searchField.getText()); }
        });
    }

    private void onAddOrUpdate() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name is required", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();
        Contact c = new Contact(name, phone.isEmpty() ? null : phone, email.isEmpty() ? null : email);
        try {
            // If contact exists, remove then add to mimic update semantics
            if (directory.getByName(name) != null) {
                directory.removeByName(name);
            }
            directory.addContact(c);
            refreshTable(searchField.getText());
        } catch (DuplicateContactException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Duplicate", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void onDeleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        Contact c = tableModel.getAt(row);
        int confirm = JOptionPane.showConfirmDialog(this, "Delete " + c.getName() + "?", "Confirm", JOptionPane.OK_CANCEL_OPTION);
        if (confirm == JOptionPane.OK_OPTION) {
            directory.removeByName(c.getName());
            refreshTable(searchField.getText());
        }
    }

    private void onSave() {
        try {
            directory.saveToFile(dataFile);
            JOptionPane.showMessageDialog(this, "Saved to " + dataFile.getPath());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Failed to save: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshTable(String filter) {
        List<Contact> list;
        if (filter == null) filter = "";
        if (filter.isEmpty()) {
            list = new ArrayList<>(directory.listAll());
        } else {
            list = new ArrayList<>(directory.search(filter));
        }
        tableModel.setData(list);
    }

    private static class ContactsTableModel extends AbstractTableModel {
        private final String[] cols = {"Name", "Phone", "Email"};
        private List<Contact> data = new ArrayList<>();

        public void setData(List<Contact> newData) { this.data = newData; fireTableDataChanged(); }
        public Contact getAt(int row) { return data.get(row); }

        @Override public int getRowCount() { return data.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }
        @Override public Object getValueAt(int r, int c) {
            Contact ct = data.get(r);
            switch (c) {
                case 0: return ct.getName();
                case 1: return ct.getPhoneNumber();
                case 2: return ct.getEmail();
                default: return null;
            }
        }
        @Override public boolean isCellEditable(int r, int c) { return false; }
    }
}
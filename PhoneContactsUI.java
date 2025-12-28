package com.example.phonedir;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PhoneContactsUI extends JFrame {
    private final PhoneDirectory directory;
    private final File dataFile;

    // Theme controls
    private Theme theme = Theme.ocean(false);
    private JComboBox<String> themeSelect;
    private JCheckBox darkToggle;

    private final JTextField searchField = new JTextField(18);
    private final JCheckBox onlyFavorites = new JCheckBox("Favorites only");
    private final JCheckBox hideBlocked = new JCheckBox("Hide blocked", true);

    private final DefaultListModel<Contact> listModel = new DefaultListModel<>();
    private final JList<Contact> contactList = new JList<>(listModel);

    // Favorites page model
    private final DefaultListModel<Contact> favoritesModel = new DefaultListModel<>();
    private final JList<Contact> favoritesList = new JList<>(favoritesModel);

    // Blocked page model
    private final DefaultListModel<Contact> blockedModel = new DefaultListModel<>();
    private final JList<Contact> blockedList = new JList<>(blockedModel);

    // Add page fields
    private final JTextField addNameField = new JTextField(18);
    private final JTextField addPhoneField = new JTextField(18);
    private final JTextField addEmailField = new JTextField(18);

    private JTabbedPane tabs;

    private final JLabel nameLabel = new JLabel();
    private final JLabel phoneLabel = new JLabel();
    private final JLabel emailLabel = new JLabel();

    private final JButton favBtn = new JButton();
    private final JButton blockBtn = new JButton();

    private GradientPanel headerGradient;

    public PhoneContactsUI(PhoneDirectory directory, File dataFile) {
        super("Contacts");
        this.directory = directory;
        this.dataFile = dataFile;
        applyTheme();
        // Clean typography across app
        Style.installGlobalFont("Segoe UI", 13);
        buildUI();
        refreshList();
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(900, 560);
        setLocationByPlatform(true);
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
        UIManager.put("control", theme.bg);
        UIManager.put("nimbusBlueGrey", Theme.darken(theme.bg, 0.1f));
        UIManager.put("nimbusFocus", theme.base);
        UIManager.put("text", theme.text);
        applyThemeColors();
    }

    private void applyThemeColors() {
        getContentPane().setBackground(theme.bg);
        searchField.setBackground(theme.dark ? Theme.darken(theme.bgAlt, 0.05f) : Color.WHITE);
        searchField.setForeground(theme.text);
        onlyFavorites.setForeground(theme.text);
        hideBlocked.setForeground(theme.text);

        if (headerGradient != null) {
            headerGradient.setColors(theme.headerStart, theme.headerEnd);
        }
        nameLabel.setForeground(Color.WHITE);
        phoneLabel.setForeground(theme.textMuted);
        emailLabel.setForeground(theme.textMuted);

        contactList.setBackground(theme.bgAlt);
        contactList.setForeground(theme.text);
        contactList.setSelectionBackground(theme.baseLight);
        contactList.setSelectionForeground(Color.WHITE);
    }

    public void applyTheme(Theme t) {
        if (t == null) return;
        this.theme = t;
        applyThemeColors();
        SwingUtilities.updateComponentTreeUI(this);
        repaint();
    }

    private void buildUI() {
        setLayout(new BorderLayout(8, 8));

        // Top search bar
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.setBorder(Style.padding(8, 12, 8, 12));
        top.add(new JLabel("Search"));
        top.add(searchField);
        onlyFavorites.addActionListener(e -> refreshList());
        hideBlocked.addActionListener(e -> refreshList());
        top.add(onlyFavorites);
        top.add(hideBlocked);
        JButton saveBtn = new JButton(new AbstractAction("Save") {
            @Override public void actionPerformed(ActionEvent e) { onSave(); }
        });
        tintButton(saveBtn, theme.base, Color.WHITE);
        top.add(saveBtn);

        // Theme controls
        top.add(new JLabel("Theme:"));
        themeSelect = new JComboBox<>(new String[]{"Ocean", "Emerald", "Amethyst", "Coral"});
        themeSelect.addActionListener(e -> onThemeChanged());
        top.add(themeSelect);
        darkToggle = new JCheckBox("Dark");
        darkToggle.addActionListener(e -> onThemeChanged());
        top.add(darkToggle);

        // Menu bar with interactive frames
        JMenuBar menuBar = new JMenuBar();
        JMenu toolsMenu = new JMenu("Tools");
        JMenuItem quickAddItem = new JMenuItem(new AbstractAction("Quick Add...") {
            @Override public void actionPerformed(ActionEvent e) { new QuickAddDialog(PhoneContactsUI.this, directory, created -> { refreshList(); selectByName(created.getName()); }).setVisible(true); }
        });
        JMenuItem themeDesignerItem = new JMenuItem(new AbstractAction("Theme Designer...") {
            @Override public void actionPerformed(ActionEvent e) { new ThemeDesignerFrame(PhoneContactsUI.this).setVisible(true); }
        });
        JMenuItem statsItem = new JMenuItem(new AbstractAction("Stats...") {
            @Override public void actionPerformed(ActionEvent e) { new StatsFrame(directory).setVisible(true); }
        });
        toolsMenu.add(quickAddItem);
        toolsMenu.add(themeDesignerItem);
        toolsMenu.add(statsItem);
        menuBar.add(toolsMenu);
        setJMenuBar(menuBar);

        add(top, BorderLayout.NORTH);

        // Center split: list (left) + details (right)
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setResizeWeight(0.35);
        split.setLeftComponent(buildLeftPane());
        split.setRightComponent(buildRightPane());

        JPanel allPanel = new JPanel(new BorderLayout());
        allPanel.add(split, BorderLayout.CENTER);

        tabs = new JTabbedPane();
        tabs.addTab("All", allPanel);
        tabs.addTab("Favorites", buildFavoritesPane());
        tabs.addTab("Blocked", buildBlockedPane());
        tabs.addTab("Add", buildAddPane());
        tabs.addTab("Stats", buildStatsPane());
        tabs.addChangeListener(e -> onTabChanged());
        add(tabs, BorderLayout.CENTER);

        // Live search
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            private void doRefresh() {
                if (tabs != null) {
                    int idx = tabs.getSelectedIndex();
                    if (idx == 1) refreshFavoritesList();
                    else if (idx == 2) refreshBlockedList();
                    else refreshList();
                } else {
                    refreshList();
                }
            }
            public void insertUpdate(DocumentEvent e) { doRefresh(); }
            public void removeUpdate(DocumentEvent e) { doRefresh(); }
            public void changedUpdate(DocumentEvent e) { doRefresh(); }
        });

        // Explicit search action (Enter) -> show popup if none found
        searchField.addActionListener(e -> {
            String q = searchField.getText();
            try {
                directory.searchOrThrow(q);
                // If found, ensure list reflects current query
                if (tabs != null && tabs.getSelectedIndex() == 1) refreshBlockedList();
                else refreshList();
            } catch (ContactNotFoundException ex) {
                JOptionPane.showMessageDialog(
                        this,
                        ex.getMessage(),
                        "No Contact Found",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }
        });

        // Keyboard shortcuts
        JRootPane root = getRootPane();
        InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = root.getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK), "add");
        am.put("add", new AbstractAction() { public void actionPerformed(ActionEvent e) { onNew(); } });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), "save");
        am.put("save", new AbstractAction() { public void actionPerformed(ActionEvent e) { onSave(); } });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
        am.put("delete", new AbstractAction() { public void actionPerformed(ActionEvent e) { onDelete(); } });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "edit");
        am.put("edit", new AbstractAction() { public void actionPerformed(ActionEvent e) { onEdit(); } });
        // New shortcuts
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "quickAdd");
        am.put("quickAdd", new AbstractAction() { public void actionPerformed(ActionEvent e) { new QuickAddDialog(PhoneContactsUI.this, directory, created -> { refreshList(); selectByName(created.getName()); }).setVisible(true); } });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK), "themeDesigner");
        am.put("themeDesigner", new AbstractAction() { public void actionPerformed(ActionEvent e) { new ThemeDesignerFrame(PhoneContactsUI.this).setVisible(true); } });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_DOWN_MASK), "stats");
        am.put("stats", new AbstractAction() { public void actionPerformed(ActionEvent e) { new StatsFrame(directory).setVisible(true); } });
    }

    private JComponent buildLeftPane() {
        JPanel left = new JPanel(new BorderLayout(6, 6));
        left.setBorder(Style.padding(8, 8, 8, 8));

        contactList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        contactList.setCellRenderer(new ContactRenderer());
        contactList.addListSelectionListener(e -> updateDetails(contactList.getSelectedValue()));
        JScrollPane listScroll = new JScrollPane(contactList);

        left.add(listScroll, BorderLayout.CENTER);
        left.add(buildAlphabetBar(), BorderLayout.EAST);

        // Right-click context menu on list
        contactList.addMouseListener(new MouseAdapter() {
            private void maybeShow(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int idx = contactList.locationToIndex(e.getPoint());
                    if (idx >= 0) contactList.setSelectedIndex(idx);
                    JPopupMenu menu = createContextMenu();
                    menu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
            @Override public void mousePressed(MouseEvent e) { maybeShow(e); }
            @Override public void mouseReleased(MouseEvent e) { maybeShow(e); }
        });
        return left;
    }

    private JComponent buildFavoritesPane() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        favoritesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        favoritesList.setCellRenderer(new ContactRenderer());
        favoritesList.addMouseListener(new MouseAdapter() {
            private void maybeShow(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int idx = favoritesList.locationToIndex(e.getPoint());
                    if (idx >= 0) favoritesList.setSelectedIndex(idx);
                    JPopupMenu menu = createContextMenu();
                    menu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
            @Override public void mousePressed(MouseEvent e) { maybeShow(e); }
            @Override public void mouseReleased(MouseEvent e) { maybeShow(e); }
        });
        panel.add(new JScrollPane(favoritesList), BorderLayout.CENTER);

        JButton unfavBtn = new JButton(new AbstractAction("Unfavorite Selected") {
            @Override public void actionPerformed(ActionEvent e) {
                Contact c = favoritesList.getSelectedValue();
                if (c == null) return;
                c.setFavorite(false);
                refreshFavoritesList();
                refreshList();
            }
        });
        tintButton(unfavBtn, theme.baseDark, Color.WHITE);
        JPanel actionsFav = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionsFav.add(unfavBtn);
        panel.add(actionsFav, BorderLayout.SOUTH);
        return panel;
    }

    private JComponent buildBlockedPane() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        blockedList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        blockedList.setCellRenderer(new ContactRenderer());
        blockedList.addMouseListener(new MouseAdapter() {
            private void maybeShow(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int idx = blockedList.locationToIndex(e.getPoint());
                    if (idx >= 0) blockedList.setSelectedIndex(idx);
                    JPopupMenu menu = createBlockedContextMenu();
                    menu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
            @Override public void mousePressed(MouseEvent e) { maybeShow(e); }
            @Override public void mouseReleased(MouseEvent e) { maybeShow(e); }
        });
        panel.add(new JScrollPane(blockedList), BorderLayout.CENTER);

        JButton unblockBtn = new JButton(new AbstractAction("Unblock Selected") {
            @Override public void actionPerformed(ActionEvent e) { onUnblockSelected(); }
        });
        tintButton(unblockBtn, theme.base, Color.WHITE);
        JButton deleteBtn = new JButton(new AbstractAction("Delete Selected") {
            @Override public void actionPerformed(ActionEvent e) { onDeleteBlockedSelected(); }
        });
        tintButton(deleteBtn, new Color(0xE05555), Color.WHITE);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(unblockBtn);
        actions.add(deleteBtn);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private JComponent buildAddPane() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6,6,6,6);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridx = 0; gc.gridy = 0; p.add(new JLabel("Name"), gc);
        gc.gridx = 1; gc.weightx = 1; p.add(addNameField, gc);
        gc.gridx = 0; gc.gridy = 1; gc.weightx = 0; p.add(new JLabel("Phone"), gc);
        gc.gridx = 1; gc.weightx = 1; p.add(addPhoneField, gc);
        gc.gridx = 0; gc.gridy = 2; gc.weightx = 0; p.add(new JLabel("Email"), gc);
        gc.gridx = 1; gc.weightx = 1; p.add(addEmailField, gc);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton reset = new JButton(new AbstractAction("Reset") {
            @Override public void actionPerformed(ActionEvent e) { addNameField.setText(""); addPhoneField.setText(""); addEmailField.setText(""); }
        });
        tintButton(reset, Theme.lighten(theme.base, 0.35f), Color.BLACK);
        JButton save = new JButton(new AbstractAction("Save Contact") {
            @Override public void actionPerformed(ActionEvent e) { onAddPageSave(); }
        });
        tintButton(save, theme.base, Color.WHITE);
        actions.add(reset);
        actions.add(save);
        gc.gridx = 0; gc.gridy = 3; gc.gridwidth = 2; gc.weightx = 0; p.add(actions, gc);
        return p;
    }

    private JComponent buildAlphabetBar() {
        JPanel letters = new JPanel();
        letters.setLayout(new GridLayout(27, 1, 0, 1));
        for (char c = 'A'; c <= 'Z'; c++) {
            final char ch = c;
            JButton b = new JButton(String.valueOf(ch));
            b.setMargin(new Insets(2,2,2,2));
            b.setFocusPainted(false);
            b.addActionListener(e -> jumpToLetter(ch));
            letters.add(b);
        }
        JButton hash = new JButton("#");
        hash.setMargin(new Insets(2,2,2,2));
        hash.addActionListener(e -> jumpToOther());
        letters.add(hash);
        return letters;
    }

    private JComponent buildRightPane() {
        JPanel right = new JPanel(new BorderLayout(8, 8));

        // Name header with favorite toggle
        headerGradient = new GradientPanel(theme.headerStart, theme.headerEnd);
        headerGradient.setLayout(new BorderLayout());
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 20f));
        headerGradient.add(nameLabel, BorderLayout.CENTER);
        JPanel toggles = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        toggles.setOpaque(false);
        favBtn.setAction(new AbstractAction("☆ Favorite") {
            @Override public void actionPerformed(ActionEvent e) { onToggleFavorite(); }
        });
        blockBtn.setAction(new AbstractAction("Block") {
            @Override public void actionPerformed(ActionEvent e) { onToggleBlock(); }
        });
        toggles.add(favBtn);
        toggles.add(blockBtn);
        headerGradient.add(toggles, BorderLayout.EAST);
        right.add(headerGradient, BorderLayout.NORTH);

        // Details
        JPanel details = new JPanel();
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        details.setBorder(Style.padding(12, 12, 12, 12));
        phoneLabel.setForeground(new Color(60, 60, 60));
        emailLabel.setForeground(new Color(60, 60, 60));
        details.add(phoneLabel);
        details.add(Box.createVerticalStrut(4));
        details.add(emailLabel);
        right.add(details, BorderLayout.CENTER);

        // Actions bar
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.setBorder(Style.padding(8, 12, 12, 12));
        JButton callBtn = new JButton(new AbstractAction("Call") { @Override public void actionPerformed(ActionEvent e) { onCall(); } });
        JButton msgBtn = new JButton(new AbstractAction("Message") { @Override public void actionPerformed(ActionEvent e) { onMessage(); } });
        JButton emailBtn = new JButton(new AbstractAction("Email") { @Override public void actionPerformed(ActionEvent e) { onEmail(); } });
        JButton shareBtn = new JButton(new AbstractAction("Share") { @Override public void actionPerformed(ActionEvent e) { onShare(); } });
        JButton editBtn = new JButton(new AbstractAction("Edit") { @Override public void actionPerformed(ActionEvent e) { onEdit(); } });
        JButton delBtn = new JButton(new AbstractAction("Delete") { @Override public void actionPerformed(ActionEvent e) { onDelete(); } });
        tintButton(callBtn, theme.base, Color.WHITE);
        tintButton(msgBtn, Theme.lighten(theme.base, 0.10f), Color.WHITE);
        tintButton(emailBtn, Theme.lighten(theme.base, 0.20f), Color.WHITE);
        tintButton(shareBtn, Theme.lighten(theme.base, 0.30f), Color.BLACK);
        tintButton(editBtn, theme.baseDark, Color.WHITE);
        tintButton(delBtn, new Color(0xE05555), Color.WHITE);
        actions.add(callBtn);
        actions.add(msgBtn);
        actions.add(emailBtn);
        actions.add(shareBtn);
        actions.add(editBtn);
        actions.add(delBtn);
        right.add(actions, BorderLayout.SOUTH);

        return right;
    }

    private void refreshList() {
        String q = searchField.getText() == null ? "" : searchField.getText().trim();
        boolean favOnly = onlyFavorites.isSelected();
        boolean hideBlk = hideBlocked.isSelected();
        List<Contact> src;
        if (q.isEmpty()) src = directory.listAll();
        else src = directory.search(q);
        listModel.clear();
        for (Contact c : src) {
            if (favOnly && !c.isFavorite()) continue;
            if (hideBlk && c.isBlocked()) continue;
            listModel.addElement(c);
        }
        if (!listModel.isEmpty()) {
            contactList.setSelectedIndex(0);
        } else {
            updateDetails(null);
        }
    }

    private void refreshFavoritesList() {
        String q = searchField.getText() == null ? "" : searchField.getText().trim();
        favoritesModel.clear();
        List<Contact> src = q.isEmpty() ? directory.listAll() : directory.search(q);
        for (Contact c : src) {
            if (c.isFavorite() && (!hideBlocked.isSelected() || !c.isBlocked())) favoritesModel.addElement(c);
        }
        if (!favoritesModel.isEmpty()) {
            favoritesList.setSelectedIndex(0);
            favoritesList.ensureIndexIsVisible(0);
        }
    }

    private void refreshBlockedList() {
        String q = searchField.getText() == null ? "" : searchField.getText().trim();
        blockedModel.clear();
        List<Contact> src = q.isEmpty() ? directory.listAll() : directory.search(q);
        for (Contact c : src) {
            if (c.isBlocked()) blockedModel.addElement(c);
        }
        if (!blockedModel.isEmpty()) {
            blockedList.setSelectedIndex(0);
            blockedList.ensureIndexIsVisible(0);
        }
    }

    private void updateDetails(Contact c) {
        if (c == null) {
            nameLabel.setText("");
            phoneLabel.setText("");
            emailLabel.setText("");
            favBtn.setEnabled(false);
            blockBtn.setEnabled(false);
            return;
        }
        nameLabel.setText(c.getName());
        phoneLabel.setText("Phone: " + (c.getPhoneNumber() == null ? "-" : c.getPhoneNumber()));
        emailLabel.setText("Email: " + (c.getEmail() == null ? "-" : c.getEmail()));
        favBtn.setEnabled(true);
        favBtn.setText(c.isFavorite() ? "★ Favorited" : "☆ Favorite");
        blockBtn.setEnabled(true);
        blockBtn.setText(c.isBlocked() ? "Unblock" : "Block");
    }

    private void onToggleFavorite() {
        Contact c = contactList.getSelectedValue();
        if (c == null) return;
        c.setFavorite(!c.isFavorite());
        updateDetails(c);
        contactList.repaint();
        if (onlyFavorites.isSelected() && !c.isFavorite()) {
            refreshList();
        }
    }

    private void onToggleBlock() {
        Contact c = contactList.getSelectedValue();
        if (c == null) return;
        c.setBlocked(!c.isBlocked());
        updateDetails(c);
        contactList.repaint();
        if (hideBlocked.isSelected() && c.isBlocked()) {
            refreshList();
        }
        refreshBlockedList();
    }

    private void onCall() {
        Contact c = contactList.getSelectedValue();
        if (c == null) return;
        String phone = c.getPhoneNumber();
        if (phone == null || phone.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "No phone number", "Call", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        // Try to open tel: link; fallback to clipboard
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI("tel:" + phone));
            } else {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(phone), null);
                JOptionPane.showMessageDialog(this, "Phone copied to clipboard: " + phone);
            }
        } catch (Exception ex) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(phone), null);
            JOptionPane.showMessageDialog(this, "Phone copied to clipboard: " + phone);
        }
    }

    private void onMessage() {
        Contact c = contactList.getSelectedValue();
        if (c == null) return;
        String phone = c.getPhoneNumber();
        if (phone == null || phone.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "No phone number", "Message", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(phone), null);
        JOptionPane.showMessageDialog(this, "Pretend sending SMS to: " + phone + "\n(Phone number copied to clipboard)");
    }

    private void onEmail() {
        Contact c = contactList.getSelectedValue();
        if (c == null) return;
        String email = c.getEmail();
        if (email == null || email.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "No email address", "Email", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().mail(new URI("mailto:" + email));
            } else {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(email), null);
                JOptionPane.showMessageDialog(this, "Email copied to clipboard: " + email);
            }
        } catch (Exception ex) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(email), null);
            JOptionPane.showMessageDialog(this, "Email copied to clipboard: " + email);
        }
    }

    private void onShare() {
        Contact c = contactList.getSelectedValue();
        if (c == null) return;
        String vcard = toVCard(c);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(vcard), null);
        JOptionPane.showMessageDialog(this, "Contact copied in vCard format to clipboard");
    }

    private static String toVCard(Contact c) {
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCARD\n");
        sb.append("VERSION:3.0\n");
        sb.append("FN:").append(c.getName()).append("\n");
        if (c.getPhoneNumber() != null) sb.append("TEL:").append(c.getPhoneNumber()).append("\n");
        if (c.getEmail() != null) sb.append("EMAIL:").append(c.getEmail()).append("\n");
        sb.append("END:VCARD\n");
        return sb.toString();
    }

    private JPopupMenu createContextMenu() {
        JPopupMenu m = new JPopupMenu();
        m.add(new JMenuItem(new AbstractAction("Add Contact") { @Override public void actionPerformed(ActionEvent e) { onNew(); } }));
        m.add(new JMenuItem(new AbstractAction("Edit") { @Override public void actionPerformed(ActionEvent e) { onEdit(); } }));
        m.add(new JMenuItem(new AbstractAction("Delete") { @Override public void actionPerformed(ActionEvent e) { onDelete(); } }));
        return m;
    }

    private JPopupMenu createBlockedContextMenu() {
        JPopupMenu m = new JPopupMenu();
        m.add(new JMenuItem(new AbstractAction("Unblock") { @Override public void actionPerformed(ActionEvent e) { onUnblockSelected(); } }));
        m.add(new JMenuItem(new AbstractAction("Delete") { @Override public void actionPerformed(ActionEvent e) { onDeleteBlockedSelected(); } }));
        return m;
    }

    private void onEdit() {
        Contact c = contactList.getSelectedValue();
        if (c == null) return;
        Contact edited = editDialog(c);
        if (edited != null) {
            edited.setFavorite(c.isFavorite());
            edited.setBlocked(c.isBlocked());
            if (!c.getName().equalsIgnoreCase(edited.getName())) {
                // name changed -> replace via remove+add
                directory.removeByName(c.getName());
            }
            try {
                directory.addContact(edited);
            } catch (DuplicateContactException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Duplicate", JOptionPane.WARNING_MESSAGE);
            }
            refreshList();
            selectByName(edited.getName());
        }
    }

    private void onNew() {
        Contact created = editDialog("", "", "");
        if (created != null) {
            try {
                directory.addContact(created);
                refreshList();
                selectByName(created.getName());
            } catch (DuplicateContactException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Duplicate", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void onDelete() {
        Contact c = contactList.getSelectedValue();
        if (c == null) return;
        int confirm = JOptionPane.showConfirmDialog(this, "Delete " + c.getName() + "?", "Confirm", JOptionPane.OK_CANCEL_OPTION);
        if (confirm == JOptionPane.OK_OPTION) {
            directory.removeByName(c.getName());
            refreshList();
            refreshBlockedList();
        }
    }

    private void onUnblockSelected() {
        Contact c = blockedList.getSelectedValue();
        if (c == null) return;
        c.setBlocked(false);
        refreshBlockedList();
        refreshList();
    }

    private void onDeleteBlockedSelected() {
        Contact c = blockedList.getSelectedValue();
        if (c == null) return;
        int confirm = JOptionPane.showConfirmDialog(this, "Delete " + c.getName() + "?", "Confirm", JOptionPane.OK_CANCEL_OPTION);
        if (confirm == JOptionPane.OK_OPTION) {
            directory.removeByName(c.getName());
            refreshBlockedList();
            refreshList();
        }
    }

    private void onAddPageSave() {
        String n = addNameField.getText().trim();
        if (n.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name is required", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String ph = addPhoneField.getText().trim(); if (ph.isEmpty()) ph = null;
        String em = addEmailField.getText().trim(); if (em.isEmpty()) em = null;
        try {
            directory.addContact(new Contact(n, ph, em));
            addNameField.setText(""); addPhoneField.setText(""); addEmailField.setText("");
            refreshList();
            tabs.setSelectedIndex(0); // switch back to All
        } catch (DuplicateContactException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Duplicate", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void onTabChanged() {
        int idx = tabs.getSelectedIndex();
        if (idx == 0) {
            refreshList();
        } else if (idx == 1) {
            refreshFavoritesList();
        } else if (idx == 2) {
            refreshBlockedList();
        } else if (idx == 4) {
            refreshStatsPanel();
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

    private void jumpToLetter(char ch) {
        int target = -1;
        String s = String.valueOf(ch).toLowerCase(Locale.ROOT);
        for (int i = 0; i < listModel.size(); i++) {
            String name = listModel.get(i).getName();
            if (name != null && name.toLowerCase(Locale.ROOT).startsWith(s)) {
                target = i; break;
            }
        }
        if (target >= 0) {
            contactList.setSelectedIndex(target);
            contactList.ensureIndexIsVisible(target);
        }
    }

    private void jumpToOther() {
        if (listModel.isEmpty()) return;
        contactList.setSelectedIndex(0);
        contactList.ensureIndexIsVisible(0);
    }

    private void onThemeChanged() {
        String name = (String) themeSelect.getSelectedItem();
        boolean dark = darkToggle.isSelected();
        if ("Emerald".equals(name)) theme = Theme.emerald(dark);
        else if ("Amethyst".equals(name)) theme = Theme.amethyst(dark);
        else if ("Coral".equals(name)) theme = Theme.coral(dark);
        else theme = Theme.ocean(dark);
        applyThemeColors();
        // repaint whole window to apply new palette
        SwingUtilities.updateComponentTreeUI(this);
        repaint();
    }

    private void selectByName(String name) {
        for (int i = 0; i < listModel.size(); i++) {
            if (listModel.get(i).getName().equalsIgnoreCase(name)) {
                contactList.setSelectedIndex(i);
                contactList.ensureIndexIsVisible(i);
                return;
            }
        }
    }

    private Contact editDialog(Contact src) {
        String n = src.getName();
        String ph = src.getPhoneNumber();
        String em = src.getEmail();
        return editDialog(n == null ? "" : n, ph == null ? "" : ph, em == null ? "" : em);
    }

    private Contact editDialog(String initialName, String initialPhone, String initialEmail) {
        JTextField name = new JTextField(initialName, 18);
        JTextField phone = new JTextField(initialPhone == null ? "" : initialPhone, 18);
        JTextField email = new JTextField(initialEmail == null ? "" : initialEmail, 18);
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4,4,4,4);
        gc.gridy = 0; gc.gridx = 0; p.add(new JLabel("Name"), gc); gc.gridx = 1; p.add(name, gc);
        gc.gridy = 1; gc.gridx = 0; p.add(new JLabel("Phone"), gc); gc.gridx = 1; p.add(phone, gc);
        gc.gridy = 2; gc.gridx = 0; p.add(new JLabel("Email"), gc); gc.gridx = 1; p.add(email, gc);
        int res = JOptionPane.showConfirmDialog(this, p, "Contact", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            String n = name.getText().trim();
            if (n.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Name is required", "Validation", JOptionPane.WARNING_MESSAGE);
                return null;
            }
            String ph = phone.getText().trim(); if (ph.isEmpty()) ph = null;
            String em = email.getText().trim(); if (em.isEmpty()) em = null;
            return new Contact(n, ph, em);
        }
        return null;
    }

    private void tintButton(AbstractButton b, Color bg, Color fg) {
        if (b == null) return;
        b.setFocusPainted(false);
        b.setBackground(bg);
        b.setForeground(fg);
    }

    private static int percent(int part, int total) {
        if (total == 0) return 0;
        return Math.round(part * 100f / total);
    }

    private static class ContactRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            lbl.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
            lbl.setOpaque(true);
            if (value instanceof Contact) {
                Contact c = (Contact) value;
                String star = c.isFavorite() ? "<span style='color:#F5B301'>★ </span>" : ""; // golden star
                String secondary = c.getPhoneNumber() != null ? c.getPhoneNumber() : (c.getEmail() != null ? c.getEmail() : "");
                String name = c.getName();
                String blocked = c.isBlocked() ? " <span style='color:#999'>(blocked)</span>" : "";
                String txt = "<html>" + star + name + blocked + (secondary.isEmpty() ? "" : "  ·  " + secondary) + "</html>";
                lbl.setText(txt);
                if (!isSelected) {
                    if (c.isBlocked()) lbl.setForeground(new Color(130,130,130));
                }
            }
            return lbl;
        }
    }
    // Stats tab (embedded panel similar to StatsFrame)
    private JPanel statsPanel;
    private JLabel totalLblTab, favLblTab, blockedLblTab;
    private JProgressBar favBarTab, blockedBarTab;

    private JComponent buildStatsPane() {
        statsPanel = new JPanel(new GridBagLayout());
        statsPanel.setBorder(Style.padding(12,12,12,12));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(8,8,8,8);
        gc.anchor = GridBagConstraints.WEST;
        totalLblTab = new JLabel();
        favLblTab = new JLabel();
        blockedLblTab = new JLabel();
        favBarTab = new JProgressBar(0,100);
        blockedBarTab = new JProgressBar(0,100);
        gc.gridx=0; gc.gridy=0; statsPanel.add(new JLabel("Total"), gc);
        gc.gridx=1; statsPanel.add(totalLblTab, gc);
        gc.gridx=0; gc.gridy=1; statsPanel.add(new JLabel("Favorites"), gc);
        gc.gridx=1; statsPanel.add(favLblTab, gc);
        gc.gridx=0; gc.gridy=2; statsPanel.add(new JLabel("Blocked"), gc);
        gc.gridx=1; statsPanel.add(blockedLblTab, gc);
        gc.gridx=0; gc.gridy=3; gc.gridwidth=2; gc.fill=GridBagConstraints.HORIZONTAL; statsPanel.add(favBarTab, gc);
        gc.gridy=4; statsPanel.add(blockedBarTab, gc);
        refreshStatsPanel();
        return statsPanel;
    }

    private void refreshStatsPanel() {
        if (statsPanel == null) return;
        int total = directory.size();
        int fav = 0, blocked = 0;
        for (Contact c : directory.listAll()) {
            if (c.isFavorite()) fav++;
            if (c.isBlocked()) blocked++;
        }
        totalLblTab.setText(String.valueOf(total));
        favLblTab.setText(fav + " (" + percent(fav, total) + "%)");
        blockedLblTab.setText(blocked + " (" + percent(blocked, total) + "%)");
        favBarTab.setValue(percent(fav, total));
        blockedBarTab.setValue(percent(blocked, total));
    }
}

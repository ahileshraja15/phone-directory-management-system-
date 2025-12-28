package com.example.phonedir;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MaterialContactsUI extends JFrame {
    private enum FilterMode { ALL, FAVORITES, BLOCKED }

    private final PhoneDirectory directory;
    private final File dataFile;

    private Theme theme = Theme.fromBase(new Color(0x16C1B5), false);

    private final JTextField search = new PlaceholderTextField("Search contacts", 22);
    private final DefaultListModel<Contact> model = new DefaultListModel<>();
    private final JList<Contact> list = new JList<>(model);

    private JTabbedPane tabs;
    private JPanel detailsPanel;
    private final JLabel detailName = new JLabel();
    private final JLabel detailPhone = new JLabel();
    private final JLabel detailEmail = new JLabel();
    private final JLabel detailFlags = new JLabel();
    private Contact currentDetail;

    private FilterMode filterMode = FilterMode.ALL;
    private final JToggleButton filterAll = new JToggleButton("All");
    private final JToggleButton filterFav = new JToggleButton("Favorites");
    private final JToggleButton filterBlocked = new JToggleButton("Blocked");

    private JPanel undoBar;
    private JLabel undoLabel;
    private JButton undoButton;
    private Timer undoTimer;
    private Contact lastDeleted;

    public MaterialContactsUI(PhoneDirectory directory, File dataFile) {
        super("Phone Directory");
        this.directory = directory;
        this.dataFile = dataFile;
        Style.installGlobalFont("Segoe UI", 13);
        build();
        getContentPane().setBackground(theme.bg);
        refresh("");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(420, 680);
        setLocationByPlatform(true);
    }

    private void build() {
        setLayout(new BorderLayout());
        GradientPanel header = new GradientPanel(theme.headerStart, theme.headerEnd);
        header.setLayout(new BorderLayout());

        // Simple theme menu in the frame menu bar
        JMenuBar mb = new JMenuBar();
        JMenu themeMenu = new JMenu("Theme");
        JMenuItem oceanItem = new JMenuItem("Ocean");
        JMenuItem emeraldItem = new JMenuItem("Emerald");
        JMenuItem amethystItem = new JMenuItem("Amethyst");
        JMenuItem coralItem = new JMenuItem("Coral");
        JCheckBoxMenuItem darkItem = new JCheckBoxMenuItem("Dark mode");
        themeMenu.add(oceanItem);
        themeMenu.add(emeraldItem);
        themeMenu.add(amethystItem);
        themeMenu.add(coralItem);
        themeMenu.addSeparator();
        themeMenu.add(darkItem);
        mb.add(themeMenu);
        setJMenuBar(mb);

        ActionListener themeAction = e -> {
            boolean dark = darkItem.isSelected();
            String cmd = e.getActionCommand();
            if ("Ocean".equals(cmd)) theme = Theme.ocean(dark);
            else if ("Emerald".equals(cmd)) theme = Theme.emerald(dark);
            else if ("Amethyst".equals(cmd)) theme = Theme.amethyst(dark);
            else if ("Coral".equals(cmd)) theme = Theme.coral(dark);
            applyThemeColors(header);
        };
        oceanItem.addActionListener(themeAction);
        emeraldItem.addActionListener(themeAction);
        amethystItem.addActionListener(themeAction);
        coralItem.addActionListener(themeAction);
        darkItem.addActionListener(e -> applyThemeColors(header));
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBorder(Style.padding(16,16,12,16));

        JLabel title = new JLabel("Phone Directory");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel searchWrap = roundedField(search);
        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        searchRow.setOpaque(false);
        searchRow.add(searchWrap);
        searchRow.setAlignmentX(Component.RIGHT_ALIGNMENT);
        // Make sure typed text is clearly visible in the search field
        search.setForeground(Color.BLACK);
        search.setCaretColor(Color.BLACK);
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refresh(search.getText()); }
            public void removeUpdate(DocumentEvent e) { refresh(search.getText()); }
            public void changedUpdate(DocumentEvent e) { refresh(search.getText()); }
        });
        // Explicit search action (Enter) -> show popup if none found
        search.addActionListener(e -> {
            String q = search.getText();
            try {
                directory.searchOrThrow(q);
                refresh(q);
            } catch (ContactNotFoundException ex) {
                JOptionPane.showMessageDialog(
                        this,
                        ex.getMessage(),
                        "No Contact Found",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }
        });

        // Filter chips: All / Favorites / Blocked
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        filterRow.setOpaque(false);
        Style.softenButtons(filterAll, filterFav, filterBlocked);
        ButtonGroup group = new ButtonGroup();
        group.add(filterAll);
        group.add(filterFav);
        group.add(filterBlocked);
        filterAll.setSelected(true);
        filterAll.addActionListener(e -> { filterMode = FilterMode.ALL; styleFilterChips(); refresh(search.getText()); });
        filterFav.addActionListener(e -> { filterMode = FilterMode.FAVORITES; styleFilterChips(); refresh(search.getText()); });
        filterBlocked.addActionListener(e -> { filterMode = FilterMode.BLOCKED; styleFilterChips(); refresh(search.getText()); });
        filterRow.add(filterAll);
        filterRow.add(filterFav);
        filterRow.add(filterBlocked);
        styleFilterChips();

        top.add(title);
        top.add(Box.createVerticalStrut(8));
        top.add(searchRow);
        top.add(Box.createVerticalStrut(6));
        top.add(filterRow);
        header.add(top, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        list.setCellRenderer(new ContactCell());
        list.setFixedCellHeight(76);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Context menu for list items
        JPopupMenu menu = new JPopupMenu();
        JMenuItem details = new JMenuItem("Details"); details.addActionListener(e -> onShowDetails());
        JMenuItem edit = new JMenuItem("Edit"); edit.addActionListener(e -> onEdit());
        JMenuItem del = new JMenuItem("Delete"); del.addActionListener(e -> onDelete());
        JMenuItem favToggle = new JMenuItem("Toggle Favorite"); favToggle.addActionListener(e -> onToggleFavorite());
        JMenuItem blockToggle = new JMenuItem("Block / Unblock"); blockToggle.addActionListener(e -> onToggleBlocked());
        menu.add(details);
        menu.add(edit);
        menu.add(del);
        menu.addSeparator();
        menu.add(favToggle);
        menu.add(blockToggle);
        list.setComponentPopupMenu(menu);

        list.addMouseListener(new MouseAdapter() {
            private void maybeShowMenu(MouseEvent e) {
                if (e.isConsumed()) return;
                int index = list.locationToIndex(e.getPoint());
                if (index < 0) return;
                list.setSelectedIndex(index);
                // If clicked in right-most area or right-click, open menu
                boolean rightClick = SwingUtilities.isRightMouseButton(e);
                boolean overflowArea = e.getX() > list.getWidth() - 60;
                if (rightClick || overflowArea) {
                    menu.show(list, e.getX(), e.getY());
                    e.consume();
                }
            }
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    onShowDetails();
                }
            }
            @Override public void mousePressed(MouseEvent e) { maybeShowMenu(e); }
            @Override public void mouseReleased(MouseEvent e) { maybeShowMenu(e); }
        });
        JScrollPane scroll = new JScrollPane(list);
        scroll.getViewport().setBackground(theme.bg);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        // Contacts tab: list + floating action button
        JPanel contactsPanel = new JPanel(new BorderLayout());
        contactsPanel.setOpaque(false);
        contactsPanel.add(scroll, BorderLayout.CENTER);
        contactsPanel.add(buildAlphabetBar(), BorderLayout.EAST);

        // Floating action button area
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.setOpaque(false);
        south.setBorder(Style.padding(8,8,12,16));
        JButton fab = new FabButton("+");
        fab.setBackground(theme.base);
        fab.setForeground(Color.WHITE);
        fab.addActionListener(e -> onAdd());
        south.add(fab);
        contactsPanel.add(south, BorderLayout.SOUTH);

        // Details tab (initially empty state)
        detailsPanel = (JPanel) buildDetailsPanel();

        tabs = new JTabbedPane();
        tabs.addTab("Contacts", contactsPanel);
        tabs.addTab("Details", detailsPanel);
        add(tabs, BorderLayout.CENTER);
        // Undo bar for recently deleted contact
        undoBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        undoBar.setBorder(Style.padding(4, 12, 4, 12));
        undoBar.setBackground(new Color(0xFFF4E5));
        undoLabel = new JLabel();
        undoLabel.setForeground(new Color(0x8A4B00));
        undoButton = new JButton("Undo");
        Style.softenButtons(undoButton);
        undoButton.addActionListener(e -> onUndoDelete());
        undoBar.add(undoLabel);
        undoBar.add(Box.createHorizontalStrut(8));
        undoBar.add(undoButton);
        undoBar.setVisible(false);
        add(undoBar, BorderLayout.SOUTH);
    }

    private void onShowDetails() {
        int idx = list.getSelectedIndex();
        if (idx < 0) return;
        Contact c = model.get(idx);
        showDetailsInTab(c);
    }

    private void onAdd() {
        MaterialAddDialog dlg = new MaterialAddDialog(this, c -> {
            try {
                directory.addContact(c);
                refresh(search.getText());
            } catch (DuplicateContactException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Duplicate", JOptionPane.WARNING_MESSAGE);
            }
        });
        dlg.setVisible(true);
    }

    private void onEdit() {
        int idx = list.getSelectedIndex();
        if (idx < 0) return;
        Contact existing = model.get(idx);
        MaterialAddDialog dlg = new MaterialAddDialog(this, c -> {
            try {
                // replace by name
                if (!existing.getName().equalsIgnoreCase(c.getName())) {
                    directory.removeByName(existing.getName());
                } else {
                    directory.removeByName(existing.getName());
                }
                directory.addContact(c);
                refresh(search.getText());
            } catch (DuplicateContactException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Duplicate", JOptionPane.WARNING_MESSAGE);
            }
        });
        dlg.setInitial(existing);
        dlg.setVisible(true);
    }

    private void onDelete() {
        int idx = list.getSelectedIndex();
        if (idx < 0) return;
        Contact c = model.get(idx);
        int conf = JOptionPane.showConfirmDialog(this, "Delete " + c.getName() + "?", "Confirm", JOptionPane.OK_CANCEL_OPTION);
        if (conf == JOptionPane.OK_OPTION) {
            lastDeleted = c;
            directory.removeByName(c.getName());
            refresh(search.getText());
            showUndoBar("Deleted " + c.getName());
        }
    }

    private void onToggleFavorite() {
        int idx = list.getSelectedIndex();
        if (idx < 0) return;
        Contact c = model.get(idx);
        c.setFavorite(!c.isFavorite());
        refresh(search.getText());
    }

    private void onToggleBlocked() {
        int idx = list.getSelectedIndex();
        if (idx < 0) return;
        Contact c = model.get(idx);
        c.setBlocked(!c.isBlocked());
        refresh(search.getText());
    }

    private void refresh(String q) {
        List<Contact> src = (q == null || q.isBlank()) ? directory.listAll() : directory.search(q);
        model.clear();
        for (Contact c : src) {
            if (filterMode == FilterMode.FAVORITES && !c.isFavorite()) continue;
            if (filterMode == FilterMode.BLOCKED && !c.isBlocked()) continue;
            model.addElement(c);
        }
        if (!model.isEmpty()) list.setSelectedIndex(0);
    }

    private JPanel roundedField(JTextField tf) {
        JPanel p = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color fill = Theme.blend(theme.bgAlt, theme.base, theme.dark ? 0.35f : 0.10f);
                g2.setColor(fill);
                g2.fillRoundRect(0,0,getWidth(),getHeight(), 16,16);
                g2.dispose();
            }
        };
        p.setBorder(Style.padding(4,10,4,10));

        JComponent icon = new SearchIcon();
        icon.setBorder(BorderFactory.createEmptyBorder(0,8,0,4));
        p.add(tf, BorderLayout.CENTER);
        p.add(icon, BorderLayout.EAST);

        // Solid background so typed text is always clearly visible
        tf.setBorder(BorderFactory.createEmptyBorder(4,0,4,0));
        tf.setOpaque(true);
        tf.setBackground(Color.WHITE);
        tf.setForeground(Color.BLACK);
        return p;
    }

    private JComponent buildDetailsPanel() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(Style.padding(16,16,16,16));

        detailName.setFont(detailName.getFont().deriveFont(Font.BOLD, 18f));
        detailName.setForeground(theme.text);
        detailPhone.setForeground(theme.textMuted);
        detailEmail.setForeground(theme.textMuted);
        detailFlags.setForeground(theme.textMuted);

        p.add(detailName);
        p.add(Box.createVerticalStrut(8));
        p.add(detailPhone);
        p.add(Box.createVerticalStrut(4));
        p.add(detailEmail);
        p.add(Box.createVerticalStrut(6));
        p.add(detailFlags);

        // quick action buttons
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton callBtn = new JButton("Call");
        JButton msgBtn = new JButton("Message");
        JButton emailBtn = new JButton("Email");
        JButton shareBtn = new JButton("Share");
        Style.softenButtons(callBtn, msgBtn, emailBtn, shareBtn);
        actions.add(callBtn);
        actions.add(msgBtn);
        actions.add(emailBtn);
        actions.add(shareBtn);
        p.add(Box.createVerticalStrut(10));
        p.add(actions);

        callBtn.addActionListener(e -> onCall(currentDetail));
        msgBtn.addActionListener(e -> onMessage(currentDetail));
        emailBtn.addActionListener(e -> onEmail(currentDetail));
        shareBtn.addActionListener(e -> onShare(currentDetail));

        // initial empty state
        showDetailsInTab(null);

        return p;
    }

    private void showDetailsInTab(Contact c) {
        currentDetail = c;
        if (c == null) {
            detailName.setText("No contact selected");
            detailPhone.setText("");
            detailEmail.setText("");
            detailFlags.setText("");
        } else {
            detailName.setText(c.getName());
            detailPhone.setText("Phone: " + (c.getPhoneNumber() == null ? "-" : c.getPhoneNumber()));
            detailEmail.setText("Email: " + (c.getEmail() == null ? "-" : c.getEmail()));
            StringBuilder sb = new StringBuilder();
            if (c.isFavorite()) sb.append("★ Favorite   ");
            if (c.isBlocked()) sb.append("Blocked");
            detailFlags.setText(sb.toString());
            detailFlags.setForeground(c.isBlocked() ? new Color(0xE05555) : theme.textMuted);
        }
        if (tabs != null) {
            tabs.setSelectedIndex(1); // Details tab
        }
    }

    private JComponent buildAlphabetBar() {
        JPanel letters = new JPanel();
        letters.setOpaque(false);
        letters.setLayout(new GridLayout(27, 1, 0, 1));
        for (char c = 'A'; c <= 'Z'; c++) {
            final char ch = c;
            JButton b = new JButton(String.valueOf(ch));
            b.setMargin(new Insets(1,2,1,2));
            b.setFocusPainted(false);
            b.setFont(b.getFont().deriveFont(10f));
            b.addActionListener(e -> jumpToLetter(ch));
            letters.add(b);
        }
        JButton hash = new JButton("#");
        hash.setMargin(new Insets(1,2,1,2));
        hash.setFocusPainted(false);
        hash.setFont(hash.getFont().deriveFont(10f));
        hash.addActionListener(e -> jumpToOther());
        letters.add(hash);
        return letters;
    }

    private void jumpToLetter(char ch) {
        int target = -1;
        String s = String.valueOf(ch).toLowerCase(Locale.ROOT);
        for (int i = 0; i < model.size(); i++) {
            String name = model.get(i).getName();
            if (name != null && name.toLowerCase(Locale.ROOT).startsWith(s)) {
                target = i;
                break;
            }
        }
        if (target >= 0) {
            list.setSelectedIndex(target);
            list.ensureIndexIsVisible(target);
            onShowDetails();
        }
    }

    private void jumpToOther() {
        if (model.isEmpty()) return;
        list.setSelectedIndex(0);
        list.ensureIndexIsVisible(0);
        onShowDetails();
    }

    private void applyThemeColors(GradientPanel header) {
        if (header != null) {
            header.setColors(theme.headerStart, theme.headerEnd);
        }
        getContentPane().setBackground(theme.bg);
        if (detailsPanel != null) detailsPanel.repaint();
        if (tabs != null) tabs.repaint();
        list.repaint();
    }

    private void showUndoBar(String msg) {
        if (undoBar == null) return;
        undoLabel.setText(msg);
        undoBar.setVisible(true);
        if (undoTimer != null) {
            undoTimer.stop();
        }
        undoTimer = new Timer(6000, e -> {
            undoBar.setVisible(false);
            lastDeleted = null;
        });
        undoTimer.setRepeats(false);
        undoTimer.start();
    }

    private void onUndoDelete() {
        if (lastDeleted == null) {
            undoBar.setVisible(false);
            return;
        }
        try {
            directory.addContact(lastDeleted);
            refresh(search.getText());
            showDetailsInTab(lastDeleted);
        } catch (DuplicateContactException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Duplicate", JOptionPane.WARNING_MESSAGE);
        }
        lastDeleted = null;
        undoBar.setVisible(false);
        if (undoTimer != null) undoTimer.stop();
    }

    // Simple search glyph painted in the search field
    private class SearchIcon extends JComponent {
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int d = Math.min(getWidth(), getHeight()) - 4;
            int x = 2, y = (getHeight() - d) / 2;
            g2.setColor(theme.textMuted);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawOval(x, y, d, d);
            g2.drawLine(x + d - 1, y + d - 1, x + d + 4, y + d + 4);
            g2.dispose();
        }

        @Override public Dimension getPreferredSize() { return new Dimension(14, 14); }
    }

    // Simple details pop-up was replaced by inline Details tab; class left out intentionally.

    // Placeholder text field used for the search bar
    private class PlaceholderTextField extends JTextField {
        private final String placeholder;
        PlaceholderTextField(String placeholder, int columns) {
            super(columns);
            this.placeholder = placeholder;
        }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty() && !isFocusOwner()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(theme.textMuted);
                FontMetrics fm = g2.getFontMetrics();
                int tw = fm.stringWidth(placeholder);
                int x = (getWidth() - tw) / 2;
                int y = (getHeight() + fm.getAscent()) / 2 - 2;
                g2.drawString(placeholder, x, y);
                g2.dispose();
            }
        }
    }

    // Custom circular floating button
    private class FabButton extends JButton {
        FabButton(String text) { super(text); setFocusPainted(false); setContentAreaFilled(false); setBorderPainted(false); setPreferredSize(new Dimension(46,46)); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground()!=null?getBackground():theme.base);
            g2.fillOval(0,0,getWidth(),getHeight());
            g2.setColor(getForeground()!=null?getForeground():Color.WHITE);
            Font f = getFont().deriveFont(Font.BOLD, 20f);
            g2.setFont(f);
            String t = getText();
            int tw = g2.getFontMetrics().stringWidth(t);
            int th = g2.getFontMetrics().getAscent();
            g2.drawString(t, (getWidth()-tw)/2, (getHeight()+th)/2-4);
            g2.dispose();
        }
        @Override public boolean contains(int x, int y) {
            int r = getWidth()/2; int cx=r, cy=r; int dx=x-cx, dy=y-cy; return dx*dx+dy*dy <= r*r;
        }
    }

    private void styleFilterChips() {
        JToggleButton[] chips = new JToggleButton[]{filterAll, filterFav, filterBlocked};
        for (JToggleButton chip : chips) {
            boolean sel = chip.isSelected();
            chip.setOpaque(true);
            chip.setFocusPainted(false);
            chip.setBorder(BorderFactory.createEmptyBorder(4,10,4,10));
            if (sel) {
                chip.setBackground(theme.base);
                chip.setForeground(Color.WHITE);
            } else {
                chip.setBackground(Theme.blend(theme.bgAlt, theme.base, 0.10f));
                chip.setForeground(theme.text);
            }
        }
    }

    private void onCall(Contact c) {
        if (c == null) return;
        String phone = c.getPhoneNumber();
        if (phone == null || phone.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "No phone number", "Call", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
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

    private void onMessage(Contact c) {
        if (c == null) return;
        String phone = c.getPhoneNumber();
        if (phone == null || phone.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "No phone number", "Message", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(phone), null);
        JOptionPane.showMessageDialog(this, "Pretend sending SMS to: " + phone + "\n(Phone number copied to clipboard)");
    }

    private void onEmail(Contact c) {
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

    private void onShare(Contact c) {
        if (c == null) return;
        StringBuilder sb = new StringBuilder();
        sb.append(c.getName());
        if (c.getPhoneNumber() != null) sb.append(" | ").append(c.getPhoneNumber());
        if (c.getEmail() != null) sb.append(" | ").append(c.getEmail());
        String summary = sb.toString();
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(summary), null);
        JOptionPane.showMessageDialog(this, "Contact summary copied to clipboard");
    }

    private class ContactCell extends JPanel implements ListCellRenderer<Contact> {
        private final JLabel name = new JLabel();
        private final JLabel phone = new JLabel();
        private final JLabel overflow = new JLabel("..."); // three-dot overflow
        private final JLabel blockedLabel = new JLabel("Blocked");
        private final Color avatarBg = theme.baseLight;
        private String initial = "";
        ContactCell() {
            setLayout(new BorderLayout(12,0));
            setBorder(Style.padding(10,12,10,12));
            JPanel left = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2=(Graphics2D)g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int d = Math.min(getWidth(), getHeight());
                    g2.setColor(avatarBg);
                    int cy = (getHeight()-40)/2;
                    g2.fillOval(0, cy, 40, 40);
                    // draw first letter of name inside the circle
                    if (initial != null && !initial.isEmpty()) {
                        g2.setColor(Color.WHITE);
                        g2.setFont(getFont().deriveFont(Font.BOLD, 16f));
                        FontMetrics fm = g2.getFontMetrics();
                        int tw = fm.stringWidth(initial);
                        int th = fm.getAscent();
                        int tx = 20 - tw/2;
                        int ty = cy + 20 + th/2 - 4;
                        g2.drawString(initial, tx, ty);
                    }
                    g2.dispose();
                }
            };
            left.setPreferredSize(new Dimension(44, 52));
            left.setOpaque(false);
            add(left, BorderLayout.WEST);

            JPanel center = new JPanel();
            center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
            name.setFont(name.getFont().deriveFont(Font.BOLD, 14f));
            phone.setForeground(theme.textMuted);

            JPanel nameRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            nameRow.setOpaque(false);
            center.add(nameRow);
            nameRow.add(name);

            center.add(Box.createVerticalStrut(2));
            center.add(phone);

            blockedLabel.setForeground(new Color(0xE05555));
            blockedLabel.setFont(blockedLabel.getFont().deriveFont(Font.PLAIN, 11f));
            center.add(blockedLabel);
            add(center, BorderLayout.CENTER);

            JPanel right = new JPanel(new BorderLayout());
            right.setOpaque(false);
            overflow.setForeground(theme.textMuted);
            overflow.setFont(overflow.getFont().deriveFont(Font.BOLD, 16f));
            overflow.setHorizontalAlignment(SwingConstants.CENTER);
            right.add(overflow, BorderLayout.CENTER);
            right.setPreferredSize(new Dimension(24, 40));
            add(right, BorderLayout.EAST);
        }
        @Override public Component getListCellRendererComponent(JList<? extends Contact> list, Contact value, int index, boolean isSelected, boolean cellHasFocus) {
            name.setText(value.getName());
            phone.setText(value.getPhoneNumber()!=null? value.getPhoneNumber(): "");
            blockedLabel.setVisible(value.isBlocked());
            // store first letter of name for avatar painting
            String n = value.getName();
            initial = (n != null && !n.isEmpty()) ? n.substring(0,1).toUpperCase() : "";
            setBackground(isSelected ? theme.baseLight : theme.bgAlt);
            name.setForeground(isSelected ? theme.baseDark : theme.text);
            phone.setForeground(isSelected ? Theme.darken(theme.textMuted, 0.1f) : theme.textMuted);
            setOpaque(true);
            return this;
        }
    }
}

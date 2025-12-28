package com.example.phonedir;

import javax.swing.*;
import java.awt.*;

public class ThemeDesignerFrame extends JFrame {
    private final PhoneContactsUI owner;
    private final JColorChooser chooser = new JColorChooser(new Color(0x2D7FF9));
    private final JCheckBox dark = new JCheckBox("Dark mode");

    public ThemeDesignerFrame(PhoneContactsUI owner) {
        super("Theme Designer");
        this.owner = owner;
        setLayout(new BorderLayout());
        ((JComponent) getContentPane()).setBorder(Style.padding(8,8,8,8));
        add(chooser, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(dark);
        JButton apply = new JButton("Apply");
        JButton close = new JButton("Close");
        bottom.add(apply);
        bottom.add(close);
        add(bottom, BorderLayout.SOUTH);

        apply.addActionListener(e -> {
            Color base = chooser.getColor();
            Theme t = Theme.fromBase(base, dark.isSelected());
            owner.applyTheme(t);
        });
        close.addActionListener(e -> dispose());

        setSize(650, 420);
        setLocationByPlatform(true);
    }
}
package com.example.phonedir;

import javax.swing.*;
import java.awt.*;

public class StatsFrame extends JFrame {
    private final PhoneDirectory directory;
    private final JLabel totalLbl = new JLabel();
    private final JLabel favLbl = new JLabel();
    private final JLabel blockedLbl = new JLabel();
    private final JProgressBar favBar = new JProgressBar(0, 100);
    private final JProgressBar blockedBar = new JProgressBar(0, 100);

    public StatsFrame(PhoneDirectory directory) {
        super("Directory Stats");
        this.directory = directory;
        setLayout(new GridBagLayout());
        ((JComponent) getContentPane()).setBorder(Style.padding(12,12,12,12));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(8,8,8,8);
        gc.anchor = GridBagConstraints.WEST;

        gc.gridx=0; gc.gridy=0; add(new JLabel("Total"), gc);
        gc.gridx=1; add(totalLbl, gc);
        gc.gridx=0; gc.gridy=1; add(new JLabel("Favorites"), gc);
        gc.gridx=1; add(favLbl, gc);
        gc.gridx=0; gc.gridy=2; add(new JLabel("Blocked"), gc);
        gc.gridx=1; add(blockedLbl, gc);

        gc.gridx=0; gc.gridy=3; gc.gridwidth=2; gc.fill=GridBagConstraints.HORIZONTAL; add(favBar, gc);
        gc.gridy=4; add(blockedBar, gc);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refresh = new JButton("Refresh");
        actions.add(refresh);
        gc.gridy=5; gc.gridwidth=2; gc.fill=GridBagConstraints.NONE; add(actions, gc);

        refresh.addActionListener(e -> refreshStats());
        refreshStats();
        setSize(360, 220);
        setLocationByPlatform(true);
    }

    private void refreshStats() {
        int total = directory.size();
        int fav = 0, blocked = 0;
        for (Contact c : directory.listAll()) {
            if (c.isFavorite()) fav++;
            if (c.isBlocked()) blocked++;
        }
        totalLbl.setText(String.valueOf(total));
        favLbl.setText(fav + " (" + pct(fav, total) + "%)");
        blockedLbl.setText(blocked + " (" + pct(blocked, total) + "%)");
        favBar.setValue(percent(fav, total));
        blockedBar.setValue(percent(blocked, total));
    }

    private static String pct(int part, int total) {
        if (total == 0) return "0";
        return String.valueOf(percent(part, total));
    }
    private static int percent(int part, int total) {
        if (total == 0) return 0;
        return Math.round(part * 100f / total);
    }
}
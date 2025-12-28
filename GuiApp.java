package com.example.phonedir;

import javax.swing.SwingUtilities;
import java.io.File;

public class GuiApp {
    private static final String DATA_PATH = "data/phonebook.ser";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PhoneDirectory dir;
            try {
                dir = PhoneDirectory.loadFromFile(new File(DATA_PATH));
            } catch (java.io.FileNotFoundException e) {
                dir = new PhoneDirectory();
            } catch (Exception e) {
                e.printStackTrace();
                dir = new PhoneDirectory();
            }
            PhoneDirectoryUI ui = new PhoneDirectoryUI(dir, new File(DATA_PATH));
            ui.setLocationByPlatform(true);
            ui.setVisible(true);
        });
    }
}
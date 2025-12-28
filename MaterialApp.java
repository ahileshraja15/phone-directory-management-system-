package com.example.phonedir;

import javax.swing.*;
import java.io.File;

public class MaterialApp {
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
            MaterialContactsUI ui = new MaterialContactsUI(dir, new File(DATA_PATH));
            ui.setVisible(true);
        });
    }
}
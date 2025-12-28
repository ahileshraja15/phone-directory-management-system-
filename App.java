package com.example.phonedir;

import java.io.File;

public class App {
    private static final String DATA_PATH = "data/phonebook.ser";

    public static void main(String[] args) {
        PhoneDirectory dir;
        try {
            dir = PhoneDirectory.loadFromFile(new File(DATA_PATH));
            System.out.println("Loaded directory with " + dir.size() + " contacts.");
        } catch (java.io.FileNotFoundException e) {
            System.out.println("No saved directory found, starting new.");
            dir = new PhoneDirectory();
        } catch (Exception e) {
            System.err.println("Failed to load directory: " + e.getMessage());
            dir = new PhoneDirectory();
        }

        try {
            dir.addContact(new Contact("Alice Smith", "555-0101", "alice@example.com"));
            dir.addContact(new Contact("Bob Jones", "555-0202", "bob@example.com"));
            // Intentional duplicate to show exception handling
            dir.addContact(new Contact("Alice Smith", "555-0101", "alice@example.com"));
        } catch (DuplicateContactException e) {
            System.out.println("Caught expected duplicate: " + e.getMessage());
        }

        System.out.println("Search results for '555': " + dir.search("555"));
        System.out.println("Search results for 'Alice': " + dir.search("Alice"));

        try {
            dir.saveToFile(new File(DATA_PATH));
            System.out.println("Saved directory to " + DATA_PATH);
        } catch (java.io.IOException e) {
            System.err.println("Failed to save: " + e.getMessage());
        }
    }
}

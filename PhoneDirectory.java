package com.example.phonedir;

import java.io.*;
import java.util.*;

public class PhoneDirectory implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<String, Contact> entries = new HashMap<>();

    public void addContact(Contact contact) throws DuplicateContactException {
        if (contact == null) throw new IllegalArgumentException("contact must not be null");
        String key = normalizeKey(contact.getName());
        if (entries.containsKey(key)) {
            throw new DuplicateContactException("Duplicate contact: " + contact.getName());
        }
        entries.put(key, contact);
    }

    public Contact getByName(String name) {
        if (name == null) return null;
        return entries.get(normalizeKey(name));
    }

    public boolean removeByName(String name) {
        if (name == null) return false;
        return entries.remove(normalizeKey(name)) != null;
    }

    public List<Contact> listAll() {
        List<Contact> list = new ArrayList<>(entries.values());
        list.sort(Comparator.comparing(Contact::getName, String.CASE_INSENSITIVE_ORDER));
        return list;
    }

    public List<Contact> search(String query) {
        List<Contact> results = new ArrayList<>();
        for (Contact c : entries.values()) {
            if (c.matches(query)) {
                results.add(c);
            }
        }
        // ensure results are in alphabetical order by name, case-insensitive
        results.sort(Comparator.comparing(Contact::getName, String.CASE_INSENSITIVE_ORDER));
        return results;
    }

    // Exception-based helpers for explicit search flows
    public List<Contact> searchOrThrow(String query) throws ContactNotFoundException {
        List<Contact> results = search(query);
        if (results == null || results.isEmpty()) {
            throw new ContactNotFoundException("No contacts found for: " + (query == null ? "" : query));
        }
        return results;
    }

    public Contact getByNameOrThrow(String name) throws ContactNotFoundException {
        Contact c = getByName(name);
        if (c == null) {
            throw new ContactNotFoundException("No contact found named: " + name);
        }
        return c;
    }

    public int size() {
        return entries.size();
    }

    private String normalizeKey(String name) {
        return name.toLowerCase(Locale.ROOT).trim();
    }

    public void saveToFile(File file) throws IOException {
        if (file == null) throw new IllegalArgumentException("file must not be null");
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            if (!parent.mkdirs()) {
                throw new IOException("Failed to create directory: " + parent);
            }
        }
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(this);
        }
    }

    public static PhoneDirectory loadFromFile(File file) throws IOException, ClassNotFoundException {
        if (file == null) throw new IllegalArgumentException("file must not be null");
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Object obj = ois.readObject();
            if (!(obj instanceof PhoneDirectory)) {
                throw new IOException("Invalid serialized data");
            }
            return (PhoneDirectory) obj;
        }
    }
}

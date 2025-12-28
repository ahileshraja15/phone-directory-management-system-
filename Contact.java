package com.example.phonedir;

public class Contact extends Person implements Searchable {
    private static final long serialVersionUID = 1L;

    private final String phoneNumber;
    private final String email;

    private boolean favorite;
    private boolean blocked;

    public Contact(String name, String phoneNumber, String email) {
        super(name);
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.favorite = false;
        this.blocked = false;
    }

    public String getPhoneNumber() { return phoneNumber; }
    public String getEmail() { return email; }

    public boolean isFavorite() { return favorite; }
    public void setFavorite(boolean favorite) { this.favorite = favorite; }
    public boolean isBlocked() { return blocked; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; }

    @Override
    public boolean matches(String query) {
        if (query == null) return false;
        String q = query.toLowerCase();
        return getName().toLowerCase().contains(q)
                || (phoneNumber != null && phoneNumber.toLowerCase().contains(q))
                || (email != null && email.toLowerCase().contains(q));
    }

    @Override
    public String toString() {
        return "Contact{name='" + getName() + "', phone='" + phoneNumber + "', email='" + email + "', favorite=" + favorite + ", blocked=" + blocked + "}";
    }
}

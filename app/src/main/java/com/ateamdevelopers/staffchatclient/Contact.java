package com.ateamdevelopers.staffchatclient;

public abstract class Contact {
    private final int id;

    public Contact(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}

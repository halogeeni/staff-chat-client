package com.ateamdevelopers.staffchatclient;

public class User extends Contact {

    private final String name;

    public User(int id, String name) {
        super(id);
        this.name = name;
    }

    public String getName() {
        return name;
    }

}

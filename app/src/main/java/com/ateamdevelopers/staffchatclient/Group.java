package com.ateamdevelopers.staffchatclient;

public class Group extends Contact {

    private String name;

    public Group(int id, String name) {
        super(id);
        this.name = name;
    }

    public String getName() {
        return name;
    }

}

package com.ateamdevelopers.staffchatclient;

public class User extends Contact {

    private final String firstname, lastname;
    // user's associated group id - not sure if we need this
    private final Integer groupId;

    public User(int id, String firstname, String lastname, Integer groupId) {
        super(id);
        this.firstname = firstname;
        this.lastname = lastname;
        this.groupId = groupId;
    }

    // getters

    public String getFirstname() {
        return firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public Integer getGroupId() {
        return groupId;
    }
}

package com.ateamdevelopers.staffchatclient;

public class User extends Contact {

    private final String name;
    // user's associated group id - not sure if we need this
    //private final Integer groupId;

    public User(int id, String name/*, Integer groupId*/) {
        super(id);
        this.name = name;
        //this.groupId = groupId;
    }

    // getters

    public String getName() {
        return name;
    }
/*
    public Integer getGroupId() {
        return groupId;
    }
    */
}

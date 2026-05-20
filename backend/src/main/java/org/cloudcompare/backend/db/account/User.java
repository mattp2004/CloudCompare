package org.cloudcompare.backend.db.account;

import org.cloudcompare.backend.db.util.Rank;

import java.util.UUID;

public class User {
    public UUID id;
    public String username;
    public String email;
    public String hashedPassword;
    public Rank rank;

    public User(UUID id, String username, String email, String hashedPassword, Rank rank){
        this.id = id;
        this.username = username;
        this.email = email;
        this.hashedPassword = hashedPassword;
        this.rank = rank;
    }

    public UUID getId(){
        return id;
    }

    public String getUsername(){
        return username;
    }

    public String getHashedPassword() {
        return hashedPassword;
    }
}

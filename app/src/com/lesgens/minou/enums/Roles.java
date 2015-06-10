package com.lesgens.minou.enums;

public enum Roles {
	User("user"),
    Admin("admin"),
    Moderator("moderator");
    
    private final String stringValue;
    
    Roles(String stringValue) {
        this.stringValue = stringValue;
    }
    
    @Override
    public String toString() {
        return stringValue;
    }
    
    public static Roles fromString(String role) {
        if (role == null) return null;
        else if (role.equals("user")) return User;
        else if (role.equals("admin")) return Admin;
        else if (role.equals("moderator")) return Moderator;
        return null;
    }

}

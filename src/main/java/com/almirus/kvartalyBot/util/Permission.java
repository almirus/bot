package com.almirus.kvartalyBot.util;

public enum Permission implements Comparable<Permission> {
    ANY, USER, ADMIN, OWNER, BANNED;

    public static Permission valueOf(Integer permission) {
        if (permission == null) {
            return null;
        }
        switch (permission) {
            case 0:
                return ANY;
            case 1:
                return USER;
            case 2:
                return ADMIN;
            case 3:
                return OWNER;
            case 4:
                return BANNED;
            default:
                throw new IllegalArgumentException("unknown Permission [" + permission + "]");
        }
    }
}

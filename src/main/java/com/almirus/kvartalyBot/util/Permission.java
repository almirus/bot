package com.almirus.kvartalyBot.util;

public enum Permission implements Comparable<Permission> {
    ANY, USER, ADMIN, OWNER, BANNED;

    public static Permission valueOf(Integer permission) {
        if (permission == null) {
            return null;
        }
        return switch (permission) {
            case 0 -> ANY;
            case 1 -> USER;
            case 2 -> ADMIN;
            case 3 -> OWNER;
            case 4 -> BANNED;
            default -> throw new IllegalArgumentException("unknown Permission [" + permission + "]");
        };
    }
}

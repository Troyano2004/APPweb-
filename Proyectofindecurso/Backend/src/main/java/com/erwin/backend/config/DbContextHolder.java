
package com.erwin.backend.config;

public class DbContextHolder {

    private static final ThreadLocal<String> DB_USER = new ThreadLocal<>();
    private static final ThreadLocal<String> DB_PASS = new ThreadLocal<>();

    public static void set(String user, String pass) {
        DB_USER.set(user);
        DB_PASS.set(pass);
    }

    public static String getUser() {
        return DB_USER.get();
    }

    public static String getPass() {
        return DB_PASS.get();
    }

    public static void clear() {
        DB_USER.remove();
        DB_PASS.remove();
    }
}




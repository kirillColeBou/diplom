package com.example.sneaker_shop;

import android.content.Context;
import android.content.SharedPreferences;
public class AuthUtils {
    private static final String PREFS_NAME = "user_auth_prefs";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PASSWORD_HASH = "password_hash";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    public static void saveUserCredentials(Context context, String email, String passwordHash) {
        SharedPreferences sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_PASSWORD_HASH, passwordHash);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();
    }

    public static boolean isUserLoggedIn(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return sharedPref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public static String[] getSavedCredentials(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String email = sharedPref.getString(KEY_EMAIL, null);
        String passwordHash = sharedPref.getString(KEY_PASSWORD_HASH, null);
        return (email != null && passwordHash != null) ? new String[]{email, passwordHash} : null;
    }

    public static void logout(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.clear();
        editor.apply();
    }
}

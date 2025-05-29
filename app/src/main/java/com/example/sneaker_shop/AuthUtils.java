package com.example.sneaker_shop;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class AuthUtils {
    public static final String PREFS_NAME = "user_auth_prefs";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PASSWORD_HASH = "password_hash";
    private static final String KEY_USER_UID = "user_uid";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    public static SharedPreferences getEncryptedSharedPreferences(Context context) {
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            return EncryptedSharedPreferences.create(
                    PREFS_NAME,
                    masterKeyAlias,
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Failed to initialize EncryptedSharedPreferences", e);
        }
    }

    public static void saveUserCredentials(Context context, String email, String passwordHash, long userUid) {
        SharedPreferences sharedPref = getEncryptedSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_PASSWORD_HASH, passwordHash);
        editor.putLong(KEY_USER_UID, userUid);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();
    }

    public static void saveUserCredentials(Context context, String email, String passwordHash) {
        saveUserCredentials(context, email, passwordHash, -1L);
    }

    public static boolean isUserLoggedIn(Context context) {
        SharedPreferences sharedPref = getEncryptedSharedPreferences(context);
        return sharedPref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public static String[] getSavedCredentials(Context context) {
        SharedPreferences sharedPref = getEncryptedSharedPreferences(context);
        String email = sharedPref.getString(KEY_EMAIL, null);
        String passwordHash = sharedPref.getString(KEY_PASSWORD_HASH, null);
        return (email != null && passwordHash != null) ?
                new String[]{email, passwordHash} : null;
    }

    public static long getCurrentUserId(Context context) {
        SharedPreferences sharedPref = getEncryptedSharedPreferences(context);
        return sharedPref.getLong(KEY_USER_UID, -1L);
    }

    public static void logout(Context context) {
        try {
            SharedPreferences sharedPref = getEncryptedSharedPreferences(context);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.clear();
            editor.apply();
        } catch (Exception e) {
           try {
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .edit()
                        .clear()
                        .apply();
            } catch (Exception fallbackEx) {

            }
        }
    }
}
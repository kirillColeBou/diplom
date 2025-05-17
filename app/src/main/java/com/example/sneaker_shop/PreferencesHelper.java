package com.example.sneaker_shop;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesHelper {
    private static final String PREFS_NAME = "app_preferences";
    private static final String KEY_SELECTED_STORE_ADDRESS = "selected_store_address";
    private static final String KEY_SELECTED_STORE_ID = "selected_store_id";

    public static void saveSelectedStore(Context context, String address, int storeId) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_SELECTED_STORE_ADDRESS, address);
        try {
            int id = storeId;
            editor.putInt(KEY_SELECTED_STORE_ID, id);
        } catch (NumberFormatException e) {
            editor.putInt(KEY_SELECTED_STORE_ID, -1);
        }
        editor.apply();
    }

    public static String getSelectedStoreAddress(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return preferences.getString(KEY_SELECTED_STORE_ADDRESS, "Выбрать магазин...");
    }

    public static int getSelectedStoreId(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return preferences.getInt(KEY_SELECTED_STORE_ID, -1);
    }

    public static void clearSelectedStore(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        preferences.edit()
                .remove(KEY_SELECTED_STORE_ADDRESS)
                .remove(KEY_SELECTED_STORE_ID)
                .apply();
    }
}
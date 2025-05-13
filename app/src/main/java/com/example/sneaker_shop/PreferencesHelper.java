package com.example.sneaker_shop;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesHelper {
    private static final String PREFS_NAME = "app_preferences";
    private static final String KEY_SELECTED_STORE_ADDRESS = "selected_store_address";

    public static void saveSelectedStoreAddress(Context context, String address) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        preferences.edit().putString(KEY_SELECTED_STORE_ADDRESS, address).apply();
    }

    public static String getSelectedStoreAddress(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return preferences.getString(KEY_SELECTED_STORE_ADDRESS, "Выбрать магазин...");
    }
}

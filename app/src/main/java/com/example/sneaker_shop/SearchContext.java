package com.example.sneaker_shop;

import android.util.Log;
import org.json.JSONObject;
import org.jsoup.Jsoup;

public class SearchContext {
    private static final String URL = "https://mgxymxiehfsptuubuqfv.supabase.co/rest/v1/search_history";
    public static void saveSearchQuery(long userUid, String query) {
        new Thread(() -> {
            try {
                JSONObject data = new JSONObject();
                data.put("user_uid", userUid);
                data.put("text", query);
                Jsoup.connect(URL)
                        .header("Authorization", UserContext.TOKEN())
                        .header("apikey", UserContext.SECRET())
                        .header("Content-Type", "application/json")
                        .header("Prefer", "return=minimal")
                        .requestBody(data.toString())
                        .ignoreContentType(true)
                        .post();
            } catch (Exception e) {
                Log.e("Supabase", "Failed to save search query: " + e.getMessage());
            }
        }).start();
    }
}
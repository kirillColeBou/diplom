package com.example.sneaker_shop;

import android.util.Log;
import org.json.JSONObject;
import org.jsoup.Jsoup;

public class SearchContext {
    private static final String URL = "https://mgxymxiehfsptuubuqfv.supabase.co/rest/v1/search_history";
    private static final String TOKEN = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im1neHlteGllaGZzcHR1dWJ1cWZ2Iiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc0NTIyNzY0NSwiZXhwIjoyMDYwODAzNjQ1fQ.LNqLc1o8I8eZUxYuFXknXZZhzN5kRh0eggmg5tItiM0";
    private static final String SECRET = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im1neHlteGllaGZzcHR1dWJ1cWZ2Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDUyMjc2NDUsImV4cCI6MjA2MDgwMzY0NX0.QXcy5Dpd4_b58-xfpvPAIgm9U8Pj6w62RW6p7NDUKyQ";

    public static void saveSearchQuery(long userUid, String query) {
        new Thread(() -> {
            try {
                JSONObject data = new JSONObject();
                data.put("user_uid", userUid);
                data.put("text", query);
                Jsoup.connect(URL)
                        .header("Authorization", TOKEN)
                        .header("apikey", SECRET)
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
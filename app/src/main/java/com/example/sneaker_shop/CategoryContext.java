package com.example.sneaker_shop;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.List;

public class CategoryContext {
    private static final String URL = "https://mgxymxiehfsptuubuqfv.supabase.co/rest/v1/categories";
    private static final String TOKEN = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im1neHlteGllaGZzcHR1dWJ1cWZ2Iiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc0NTIyNzY0NSwiZXhwIjoyMDYwODAzNjQ1fQ.LNqLc1o8I8eZUxYuFXknXZZhzN5kRh0eggmg5tItiM0";
    private static final String SECRET = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im1neHlteGllaGZzcHR1dWJ1cWZ2Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDUyMjc2NDUsImV4cCI6MjA2MDgwMzY0NX0.QXcy5Dpd4_b58-xfpvPAIgm9U8Pj6w62RW6p7NDUKyQ";

    public interface CategoriesCallback {
        void onSuccess(List<Category> categories);
        void onError(String error);
    }

    public static void loadCategories(CategoriesCallback callback) {
        new LoadCategoriesTask(callback).execute();
    }

    private static class LoadCategoriesTask extends AsyncTask<Void, Void, List<Category>> {
        private final CategoriesCallback callback;
        private String error;

        LoadCategoriesTask(CategoriesCallback callback) {
            this.callback = callback;
        }

        @Override
        protected List<Category> doInBackground(Void... voids) {
            List<Category> categories = new ArrayList<>();
            categories.add(new Category(-1, "Для вас", true));
            categories.add(new Category(-2, "Все модели", false));
            try {
                Document doc = Jsoup.connect(URL)
                        .header("Authorization", TOKEN)
                        .header("apikey", SECRET)
                        .ignoreContentType(true)
                        .get();
                String response = doc.body().text();
                JSONArray jsonArray = new JSONArray(response);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    int id = obj.getInt("id");
                    String name = obj.getString("name");
                    categories.add(new Category(id, name, false));
                }
                return categories;
            } catch (Exception e) {
                error = "Error: " + e.getMessage();
                Log.e("Supabase", "Failed to load categories: " + error);
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<Category> categories) {
            if (error != null) {
                callback.onError(error);
            } else {
                callback.onSuccess(categories);
            }
        }
    }
}

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
            categories.add(new Category(-1, "Для вас", false));
            categories.add(new Category(-2, "Все модели", true));
            try {
                Document doc = Jsoup.connect(URL)
                        .header("Authorization", UserContext.TOKEN())
                        .header("apikey", UserContext.SECRET())
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

package com.example.sneaker_shop;

import android.os.AsyncTask;
import android.util.Log;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.net.URLEncoder;
import org.jsoup.Connection;

public class FavoriteContext {
    private static final String URL = "https://mgxymxiehfsptuubuqfv.supabase.co/rest/v1/favorites";
    private static final String TOKEN = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im1neHlteGllaGZzcHR1dWJ1cWZ2Iiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc0NTIyNzY0NSwiZXhwIjoyMDYwODAzNjQ1fQ.LNqLc1o8I8eZUxYuFXknXZZhzN5kRh0eggmg5tItiM0";
    private static final String SECRET = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im1neHlteGllaGZzcHR1dWJ1cWZ2Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDUyMjc2NDUsImV4cCI6MjA2MDgwMzY0NX0.QXcy5Dpd4_b58-xfpvPAIgm9U8Pj6w62RW6p7NDUKyQ";

    public interface FavoriteCallback {
        void onSuccess(boolean isFavorite);
        void onError(String error);
    }

    public static void checkFavorite(String userId, String productId, FavoriteCallback callback) {
        if (userId == null || productId == null) {
            callback.onError("User ID or Product ID is null");
            return;
        }
        new CheckFavoriteTask(userId, productId, callback).execute();
    }

    private static class CheckFavoriteTask extends AsyncTask<Void, Void, Boolean> {
        private final String userId;
        private final String productId;
        private final FavoriteCallback callback;
        private String error;

        CheckFavoriteTask(String userId, String productId, FavoriteCallback callback) {
            this.userId = userId;
            this.productId = productId;
            this.callback = callback;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                String url = URL + "?and=" +
                        URLEncoder.encode("(user_id.eq." + userId +
                                ",product_id.eq." + productId + ")", "UTF-8");
                Document doc = Jsoup.connect(url)
                        .header("Authorization", TOKEN)
                        .header("apikey", SECRET)
                        .ignoreContentType(true)
                        .get();
                return doc.body().text().length() > 2;
            } catch (Exception e) {
                error = "Error checking favorite: " + e.getMessage();
                Log.e("FavoriteContext", error);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean isFavorite) {
            if (error != null) {
                callback.onError(error);
            } else {
                callback.onSuccess(isFavorite);
            }
        }
    }

    public static void toggleFavorite(String userId, String productId, boolean isCurrentlyFavorite, FavoriteCallback callback) {
        new ToggleFavoriteTask(userId, productId, isCurrentlyFavorite, callback).execute();
    }

    private static class ToggleFavoriteTask extends AsyncTask<Void, Void, Boolean> {
        private final String userId;
        private final String productId;
        private final boolean isCurrentlyFavorite;
        private final FavoriteCallback callback;
        private String error;

        ToggleFavoriteTask(String userId, String productId, boolean isCurrentlyFavorite, FavoriteCallback callback) {
            this.userId = userId;
            this.productId = productId;
            this.isCurrentlyFavorite = isCurrentlyFavorite;
            this.callback = callback;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                if (isCurrentlyFavorite) {
                    // Удаляем из избранного
                    String url = URL + "?user_id=eq." + userId + "&product_id=eq." + productId;
                    Connection.Response response = Jsoup.connect(url)
                            .header("Authorization", TOKEN)
                            .header("apikey", SECRET)
                            .ignoreContentType(true)
                            .method(Connection.Method.DELETE)
                            .execute();
                    return !(response.statusCode() == 200 || response.statusCode() == 204);
                } else {
                    // Добавляем в избранное
                    String json = String.format("{\"user_id\":\"%s\",\"product_id\":\"%s\"}", userId, productId);
                    Connection.Response response = Jsoup.connect(URL)
                            .header("Authorization", TOKEN)
                            .header("apikey", SECRET)
                            .header("Content-Type", "application/json")
                            .header("Prefer", "return=minimal")
                            .requestBody(json)
                            .ignoreContentType(true)
                            .method(Connection.Method.POST)
                            .execute();
                    return response.statusCode() == 201;
                }
            } catch (Exception e) {
                error = "Error toggling favorite: " + e.getMessage();
                Log.e("FavoriteContext", error);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (error != null) {
                callback.onError(error);
            } else if (success != null) {
                callback.onSuccess(!isCurrentlyFavorite);
            } else {
                callback.onError("Unknown error occurred");
            }
        }
    }
}

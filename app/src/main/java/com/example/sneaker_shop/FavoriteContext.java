package com.example.sneaker_shop;

import android.os.AsyncTask;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Connection;

public class FavoriteContext {
    private static final String URL = "https://mgxymxiehfsptuubuqfv.supabase.co/rest/v1/favorites";
    public interface FavoriteCallback {
        void onSuccess(boolean isFavorite);
        void onError(String error);
    }

    public static void checkFavorite(Long userUid, String productId, FavoriteCallback callback) {
        if (userUid == -1L || productId == null) {
            callback.onError("User ID or Product ID is null");
            return;
        }
        new CheckFavoriteTask(userUid, productId, callback).execute();
    }

    private static class CheckFavoriteTask extends AsyncTask<Void, Void, Boolean> {
        private final Long userUid;
        private final String productId;
        private final FavoriteCallback callback;
        private String error;

        CheckFavoriteTask(Long userUid, String productId, FavoriteCallback callback) {
            this.userUid = userUid;
            this.productId = productId;
            this.callback = callback;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                String url = URL + "?and=" +
                        URLEncoder.encode("(user_uid.eq." + userUid +
                                ",product_id.eq." + productId + ")", "UTF-8");
                Document doc = Jsoup.connect(url)
                        .header("Authorization", UserContext.TOKEN())
                        .header("apikey", UserContext.SECRET())
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

    public static void toggleFavorite(long userUid, String productId, boolean isCurrentlyFavorite, FavoriteCallback callback) {
        new ToggleFavoriteTask(userUid, productId, isCurrentlyFavorite, callback).execute();
    }

    private static class ToggleFavoriteTask extends AsyncTask<Void, Void, Boolean> {
        private final long userUid;
        private final String productId;
        private final boolean isCurrentlyFavorite;
        private final FavoriteCallback callback;
        private String error;

        ToggleFavoriteTask(long userUid, String productId, boolean isCurrentlyFavorite, FavoriteCallback callback) {
            this.userUid = userUid;
            this.productId = productId;
            this.isCurrentlyFavorite = isCurrentlyFavorite;
            this.callback = callback;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                if (isCurrentlyFavorite) {
                    String url = URL + "?user_uid=eq." + userUid + "&product_id=eq." + URLEncoder.encode(productId, "UTF-8");
                    Connection.Response response = Jsoup.connect(url)
                            .header("Authorization", UserContext.TOKEN())
                            .header("apikey", UserContext.SECRET())
                            .ignoreContentType(true)
                            .method(Connection.Method.DELETE)
                            .execute();
                    return response.statusCode() == 204 || response.statusCode() == 200;
                } else {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("user_uid", userUid);
                    jsonObject.put("product_id", productId);
                    Connection.Response response = Jsoup.connect(URL)
                            .header("Authorization", UserContext.TOKEN())
                            .header("apikey", UserContext.SECRET())
                            .header("Content-Type", "application/json")
                            .header("Prefer", "return=minimal")
                            .requestBody(jsonObject.toString())
                            .ignoreContentType(true)
                            .method(Connection.Method.POST)
                            .execute();
                    return response.statusCode() == 201;
                }
            } catch (Exception e) {
                error = "Error toggling favorite: " + e.getMessage();
                Log.e("FavoriteContext", error);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (error != null) {
                callback.onError(error);
            } else {
                callback.onSuccess(success ? !isCurrentlyFavorite : isCurrentlyFavorite);
            }
        }
    }

    public interface FavoritesCallback {
        void onSuccess(List<Product> products);
        void onError(String error);
    }

    public static void loadFavorites(Long userUid, FavoritesCallback callback) {
        new LoadFavoritesTask(userUid, callback).execute();
    }

    private static class LoadFavoritesTask extends AsyncTask<Void, Void, List<Product>> {
        private final Long userUid;
        private final FavoritesCallback callback;
        private String error;

        LoadFavoritesTask(Long userUid, FavoritesCallback callback) {
            this.userUid = userUid;
            this.callback = callback;
        }

        @Override
        protected List<Product> doInBackground(Void... voids) {
            try {
                String url = URL + "?select=product:products(*)&user_uid=eq." + userUid;
                Document doc = Jsoup.connect(url)
                        .header("Authorization", UserContext.TOKEN())
                        .header("apikey", UserContext.SECRET())
                        .ignoreContentType(true)
                        .get();
                String response = doc.body().text();
                JSONArray jsonArray = new JSONArray(response);
                List<Product> products = new ArrayList<>();
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i).getJSONObject("product");
                    Product product = new Product(
                            obj.getInt("id"),
                            obj.getString("name"),
                            obj.getDouble("price"),
                            obj.getString("description"),
                            obj.getInt("category_id"),
                            obj.getInt("brand_id"),
                            obj.getInt("shoe_color_id"),
                            obj.getInt("sole_color_id")
                    );
                    products.add(product);
                }
                return products;
            } catch (Exception e) {
                error = "Error loading favorites: " + e.getMessage();
                Log.e("FavoriteContext", error);
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<Product> products) {
            if (error != null) {
                callback.onError(error);
            } else {
                callback.onSuccess(products);
            }
        }
    }
}

package com.example.sneaker_shop;

import android.os.AsyncTask;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.util.ArrayList;
import java.util.List;

public class ProductContext {
    private static final String URL = "https://mgxymxiehfsptuubuqfv.supabase.co/rest/v1/products";
    public interface ProductsCallback {
        void onSuccess(List<Product> products);
        void onError(String error);
    }

    public static void loadProducts(ProductsCallback callback) {
        new LoadProductsTask(callback).execute();
    }

    private static class LoadProductsTask extends AsyncTask<Void, Void, List<Product>> {
        private final ProductsCallback callback;
        private String error;

        LoadProductsTask(ProductsCallback callback) {
            this.callback = callback;
        }

        @Override
        protected List<Product> doInBackground(Void... voids) {
            List<Product> products = new ArrayList<>();
            try {
                Document doc = Jsoup.connect(URL + "?select=id,name,brand_id,description,price,category_id,shoe_color_id,sole_color_id")
                        .header("Authorization", UserContext.TOKEN())
                        .header("apikey", UserContext.SECRET())
                        .ignoreContentType(true)
                        .get();
                String response = doc.body().text();
                Log.d("SupabaseResponse", "Response: " + response);
                JSONArray jsonArray = new JSONArray(response);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
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
                error = "Error loading products: " + e.getMessage();
                Log.e("ProductContext", error, e);
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

    public static void loadProductsByCategory(int categoryId, ProductsCallback callback) {
        new LoadProductsByCategoryTask(categoryId, callback).execute();
    }

    private static class LoadProductsByCategoryTask extends AsyncTask<Void, Void, List<Product>> {
        private final int categoryId;
        private final ProductsCallback callback;
        private String error;

        LoadProductsByCategoryTask(int categoryId, ProductsCallback callback) {
            this.categoryId = categoryId;
            this.callback = callback;
        }

        @Override
        protected List<Product> doInBackground(Void... voids) {
            List<Product> products = new ArrayList<>();
            try {
                String url = URL + "?category_id=eq." + categoryId + "&select=id,name,brand_id,description,price,category_id,shoe_color_id,sole_color_id";
                Document doc = Jsoup.connect(url)
                        .header("Authorization", UserContext.TOKEN())
                        .header("apikey", UserContext.SECRET())
                        .ignoreContentType(true)
                        .get();
                String response = doc.body().text();
                JSONArray jsonArray = new JSONArray(response);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
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
                error = "Error loading products by category: " + e.getMessage();
                Log.e("Supabase", error, e);
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

    public static void loadRecommendedProducts(long userUid, ProductsCallback callback) {
        new LoadRecommendedProductsTask(userUid, callback).execute();
    }

    private static class LoadRecommendedProductsTask extends AsyncTask<Void, Void, List<Product>> {
        private final long userUid;
        private final ProductsCallback callback;
        private String error;

        LoadRecommendedProductsTask(long userUid, ProductsCallback callback) {
            this.userUid = userUid;
            this.callback = callback;
        }

        @Override
        protected List<Product> doInBackground(Void... voids) {
            try {
                String url = "https://mgxymxiehfsptuubuqfv.supabase.co/rest/v1/rpc/get_recommend_products";
                JSONObject params = new JSONObject();
                params.put("input_user_uid", userUid);
                Document doc = Jsoup.connect(url)
                        .header("Authorization", UserContext.TOKEN())
                        .header("apikey", UserContext.SECRET())
                        .header("Content-Type", "application/json")
                        .requestBody(params.toString())
                        .ignoreContentType(true)
                        .post();
                String response = doc.body().text();
                Log.d("API_RESPONSE", "Received: " + response);
                JSONArray jsonArray = new JSONArray(response);
                List<Product> products = new ArrayList<>();
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    products.add(new Product(
                            obj.getInt("id"),
                            obj.getString("name"),
                            obj.getDouble("price"),
                            obj.getString("description"),
                            obj.getInt("category_id"),
                            obj.getInt("brand_id"),
                            obj.getInt("shoe_color_id"),
                            obj.getInt("sole_color_id")
                    ));
                }
                return products;
            } catch (Exception e) {
                error = "Error fetching recommendations: " + e.getMessage();
                Log.e("API_ERROR", error, e);
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
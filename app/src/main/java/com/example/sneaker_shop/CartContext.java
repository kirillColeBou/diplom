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

public class CartContext {
    private static final String BASKET_URL = "https://mgxymxiehfsptuubuqfv.supabase.co/rest/v1/basket";
    private static final String BASKET_ITEMS_URL = "https://mgxymxiehfsptuubuqfv.supabase.co/rest/v1/basket_items";
    private static final String TOKEN = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im1neHlteGllaGZzcHR1dWJ1cWZ2Iiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc0NTIyNzY0NSwiZXhwIjoyMDYwODAzNjQ1fQ.LNqLc1o8I8eZUxYuFXknXZZhzN5kRh0eggmg5tItiM0";
    private static final String SECRET = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im1neHlteGllaGZzcHR1dWJ1cWZ2Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDUyMjc2NDUsImV4cCI6MjA2MDgwMzY0NX0.QXcy5Dpd4_b58-xfpvPAIgm9U8Pj6w62RW6p7NDUKyQ";

    public interface BasketCallback {
        void onSuccess(String basketId);
        void onError(String error);
    }

    public interface AddToBasketCallback {
        void onSuccess();
        void onError(String error);
    }

    public interface LoadCartCallback {
        void onSuccess(List<CartItem> items);
        void onError(String error);
    }

    public interface UpdateCartCallback {
        void onSuccess();
        void onError(String error);
    }

    public static void getUserBasket(String userId, BasketCallback callback) {
        new GetBasketTask(userId, callback).execute();
    }

    public static void addToBasket(String basketId, String productId, int quantity, AddToBasketCallback callback) {
        new AddToBasketTask(basketId, productId, quantity, callback).execute();
    }

    public static void loadCartItems(String basketId, LoadCartCallback callback) {
        new LoadCartItemsTask(basketId, callback).execute();
    }

    public static void updateCartItem(String itemId, int newCount, UpdateCartCallback callback) {
        new UpdateCartItemTask(itemId, newCount, callback).execute();
    }

    private static class GetBasketTask extends AsyncTask<Void, Void, String> {
        private final String userId;
        private final BasketCallback callback;
        private String error;

        GetBasketTask(String userId, BasketCallback callback) {
            this.userId = userId;
            this.callback = callback;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                String url = BASKET_URL + "?user_id=eq." + URLEncoder.encode(userId, "UTF-8") + "&select=id";
                Document doc = Jsoup.connect(url)
                        .header("Authorization", TOKEN)
                        .header("apikey", SECRET)
                        .ignoreContentType(true)
                        .get();
                JSONArray jsonArray = new JSONArray(doc.body().text());
                if (jsonArray.length() > 0) {
                    return jsonArray.getJSONObject(0).getString("id");
                } else {
                    JSONObject newBasket = new JSONObject();
                    newBasket.put("user_id", userId);
                    Document created = Jsoup.connect(BASKET_URL)
                            .header("Authorization", TOKEN)
                            .header("apikey", SECRET)
                            .header("Content-Type", "application/json")
                            .header("Prefer", "return=representation")
                            .requestBody(newBasket.toString())
                            .ignoreContentType(true)
                            .post();
                    JSONArray createdArray = new JSONArray(created.body().text());
                    return createdArray.getJSONObject(0).getString("id");
                }
            } catch (Exception e) {
                error = "Error: " + e.getMessage();
                Log.e("CartContext", error);
                return null;
            }
        }

        @Override
        protected void onPostExecute(String basketId) {
            if (error != null) {
                callback.onError(error);
            } else if (basketId != null) {
                callback.onSuccess(basketId);
            } else {
                callback.onError("Basket not found and could not be created");
            }
        }
    }

    private static class AddToBasketTask extends AsyncTask<Void, Void, Boolean> {
        private final String basketId;
        private final String productId;
        private final int quantity;
        private final AddToBasketCallback callback;
        private String error;

        AddToBasketTask(String basketId, String productId, int quantity, AddToBasketCallback callback) {
            this.basketId = basketId;
            this.productId = productId;
            this.quantity = quantity;
            this.callback = callback;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                String checkUrl = BASKET_ITEMS_URL + "?basket_id=eq." + basketId +
                        "&product_sizes_id=eq." + productId + "&select=id,count";
                Document checkDoc = Jsoup.connect(checkUrl)
                        .header("Authorization", TOKEN)
                        .header("apikey", SECRET)
                        .ignoreContentType(true)
                        .get();
                JSONArray items = new JSONArray(checkDoc.body().text());
                if (items.length() > 0) {
                    JSONObject item = items.getJSONObject(0);
                    String itemId = item.getString("id");
                    int currentCount = item.getInt("count");
                    JSONObject update = new JSONObject();
                    update.put("count", currentCount + quantity);
                    Jsoup.connect(BASKET_ITEMS_URL + "?id=eq." + itemId)
                            .header("Authorization", TOKEN)
                            .header("apikey", SECRET)
                            .header("Content-Type", "application/json")
                            .requestBody(update.toString())
                            .ignoreContentType(true)
                            .method(org.jsoup.Connection.Method.PATCH)
                            .execute();
                } else {
                    JSONObject newItem = new JSONObject();
                    newItem.put("basket_id", basketId);
                    newItem.put("count", quantity);
                    Jsoup.connect(BASKET_ITEMS_URL)
                            .header("Authorization", TOKEN)
                            .header("apikey", SECRET)
                            .header("Content-Type", "application/json")
                            .header("Prefer", "return=minimal")
                            .requestBody(newItem.toString())
                            .ignoreContentType(true)
                            .post();
                }
                return true;
            } catch (Exception e) {
                error = "Error: " + e.getMessage();
                Log.e("CartContext", error);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (error != null) {
                callback.onError(error);
            } else if (success) {
                callback.onSuccess();
            } else {
                callback.onError("Failed to add to basket");
            }
        }
    }

    private static class LoadCartItemsTask extends AsyncTask<Void, Void, List<CartItem>> {
        private final String basketId;
        private final LoadCartCallback callback;
        private String error;

        LoadCartItemsTask(String basketId, LoadCartCallback callback) {
            this.basketId = basketId;
            this.callback = callback;
        }

        @Override
        protected List<CartItem> doInBackground(Void... voids) {
            try {
                String url = BASKET_ITEMS_URL + "?basket_id=eq." + basketId +
                        "&select=id,count,product_sizes_id(*,products(*))";
                Document doc = Jsoup.connect(url)
                        .header("Authorization", TOKEN)
                        .header("apikey", SECRET)
                        .ignoreContentType(true)
                        .get();
                JSONArray itemsArray = new JSONArray(doc.body().text());
                List<CartItem> items = new ArrayList<>();
                for (int i = 0; i < itemsArray.length(); i++) {
                    JSONObject itemObj = itemsArray.getJSONObject(i);
                    JSONObject productSizeObj = itemObj.getJSONObject("product_sizes_id");
                    JSONObject obj = productSizeObj.getJSONObject("products");
                    Product product = new Product(
                            obj.getInt("id"),
                            obj.getString("name"),
                            obj.getDouble("price"),
                            obj.getString("image"),
                            obj.getString("description"),
                            obj.getInt("category_id")
                    );
                    items.add(new CartItem(
                            itemObj.getString("id"),
                            product,
                            itemObj.getInt("count")
                    ));
                }
                return items;
            } catch (Exception e) {
                error = "Error: " + e.getMessage();
                Log.e("CartContext", error);
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<CartItem> items) {
            if (error != null) {
                callback.onError(error);
            } else if (items != null) {
                callback.onSuccess(items);
            } else {
                callback.onError("Failed to load cart items");
            }
        }
    }

    private static class UpdateCartItemTask extends AsyncTask<Void, Void, Boolean> {
        private final String itemId;
        private final int newCount;
        private final UpdateCartCallback callback;
        private String error;

        UpdateCartItemTask(String itemId, int newCount, UpdateCartCallback callback) {
            this.itemId = itemId;
            this.newCount = newCount;
            this.callback = callback;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                JSONObject update = new JSONObject();
                update.put("count", newCount);
                Jsoup.connect(BASKET_ITEMS_URL + "?id=eq." + itemId)
                        .header("Authorization", TOKEN)
                        .header("apikey", SECRET)
                        .header("Content-Type", "application/json")
                        .requestBody(update.toString())
                        .ignoreContentType(true)
                        .method(org.jsoup.Connection.Method.PATCH)
                        .execute();
                return true;
            } catch (Exception e) {
                error = "Error: " + e.getMessage();
                Log.e("CartContext", error);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (error != null) {
                callback.onError(error);
            } else if (success) {
                callback.onSuccess();
            } else {
                callback.onError("Failed to update cart item");
            }
        }
    }

    public static void removeFromCart(String itemId, UpdateCartCallback callback) {
        new RemoveFromCartTask(itemId, callback).execute();
    }

    private static class RemoveFromCartTask extends AsyncTask<Void, Void, Boolean> {
        private final String itemId;
        private final UpdateCartCallback callback;
        private String error;

        RemoveFromCartTask(String itemId, UpdateCartCallback callback) {
            this.itemId = itemId;
            this.callback = callback;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                Jsoup.connect(BASKET_ITEMS_URL + "?id=eq." + itemId)
                        .header("Authorization", TOKEN)
                        .header("apikey", SECRET)
                        .ignoreContentType(true)
                        .method(org.jsoup.Connection.Method.DELETE)
                        .execute();
                return true;
            } catch (Exception e) {
                error = "Error: " + e.getMessage();
                Log.e("CartContext", error);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (error != null) {
                callback.onError(error);
            } else if (success) {
                callback.onSuccess();
            } else {
                callback.onError("Failed to remove from cart");
            }
        }
    }
}
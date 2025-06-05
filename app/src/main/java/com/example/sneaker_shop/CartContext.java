package com.example.sneaker_shop;

import android.os.AsyncTask;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.util.ArrayList;
import java.util.List;

public class CartContext {
    private static final String BASKET_URL = "https://mgxymxiehfsptuubuqfv.supabase.co/rest/v1/baskets";
    private static final String BASKET_ITEMS_URL = "https://mgxymxiehfsptuubuqfv.supabase.co/rest/v1/basket_items";
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

    public static void getUserBasket(long userId, BasketCallback callback) {
        new GetBasketTask(userId, callback).execute();
    }

    public static void addToBasket(String basketId, String productSizeId, int quantity, int storeId, AddToBasketCallback callback) {
        new AddToBasketTask(basketId, productSizeId, quantity, storeId, callback).execute();
    }

    public static void loadCartItems(String basketId, LoadCartCallback callback) {
        new LoadCartItemsTask(basketId, callback).execute();
    }

    public static void updateCartItem(String itemId, int newCount, UpdateCartCallback callback) {
        new UpdateCartItemTask(itemId, newCount, callback).execute();
    }

    public static void loadSimpleCartItems(String filter, LoadCartCallback callback) {
        new LoadSimpleCartItemsTask(filter, callback).execute();
    }

    public static void clearCart(String basketId, UpdateCartCallback callback) {
        new ClearCartTask(basketId, callback).execute();
    }

    private static class GetBasketTask extends AsyncTask<Void, Void, String> {
        private final long userUid;
        private final BasketCallback callback;
        private String error;

        GetBasketTask(long userUid, BasketCallback callback) {
            this.userUid = userUid;
            this.callback = callback;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                String url = BASKET_URL + "?user_uid=eq." + userUid + "&select=id";
                Document doc = Jsoup.connect(url)
                        .header("Authorization", UserContext.TOKEN())
                        .header("apikey", UserContext.SECRET())
                        .ignoreContentType(true)
                        .get();
                JSONArray jsonArray = new JSONArray(doc.body().text());
                if (jsonArray.length() > 0) {
                    return jsonArray.getJSONObject(0).getString("id");
                } else {
                    JSONObject newBasket = new JSONObject();
                    newBasket.put("user_uid", userUid);
                    Document created = Jsoup.connect(BASKET_URL)
                            .header("Authorization", UserContext.TOKEN())
                            .header("apikey", UserContext.SECRET())
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
        private static final int MAX_QUANTITY = 10;
        private final String basketId;
        private final String productSizeId;
        private final int quantity;
        private final int storeId;
        private final AddToBasketCallback callback;
        private String error;

        AddToBasketTask(String basketId, String productSizeId, int quantity, int storeId, AddToBasketCallback callback) {
            this.basketId = basketId;
            this.productSizeId = productSizeId;
            this.quantity = quantity;
            this.storeId = storeId;
            this.callback = callback;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                String checkUrl = BASKET_ITEMS_URL + "?basket_id=eq." + basketId +
                        "&product_size_id=eq." + productSizeId +
                        "&store_id=eq." + storeId +
                        "&select=id,count";
                Document checkDoc = Jsoup.connect(checkUrl)
                        .header("Authorization", UserContext.TOKEN())
                        .header("apikey", UserContext.SECRET())
                        .ignoreContentType(true)
                        .get();
                JSONArray items = new JSONArray(checkDoc.body().text());
                int currentCount = 0;
                String itemId = null;
                if (items.length() > 0) {
                    JSONObject item = items.getJSONObject(0);
                    itemId = item.getString("id");
                    currentCount = item.getInt("count");
                }
                String stockUrl = "https://mgxymxiehfsptuubuqfv.supabase.co/rest/v1/product_size?id=eq." + productSizeId + "&select=count";
                Document stockDoc = Jsoup.connect(stockUrl)
                        .header("Authorization", UserContext.TOKEN())
                        .header("apikey", UserContext.SECRET())
                        .ignoreContentType(true)
                        .get();
                JSONArray stockArray = new JSONArray(stockDoc.body().text());
                if (stockArray.length() == 0) {
                    error = "Размер не найден";
                    return false;
                }
                int availableStock = stockArray.getJSONObject(0).getInt("count");
                int maxAllowed = Math.min(MAX_QUANTITY, availableStock);

                if (currentCount + quantity > maxAllowed) {
                    error = "Нельзя добавить больше " + maxAllowed + " единиц";
                    return false;
                }
                if (items.length() > 0) {
                    JSONObject update = new JSONObject();
                    update.put("count", currentCount + quantity);
                    Jsoup.connect(BASKET_ITEMS_URL + "?id=eq." + itemId)
                            .header("Authorization", UserContext.TOKEN())
                            .header("apikey", UserContext.SECRET())
                            .header("Content-Type", "application/json")
                            .requestBody(update.toString())
                            .ignoreContentType(true)
                            .method(org.jsoup.Connection.Method.PATCH)
                            .execute();
                } else {
                    JSONObject newItem = new JSONObject();
                    newItem.put("basket_id", basketId);
                    newItem.put("product_size_id", productSizeId);
                    newItem.put("count", quantity);
                    newItem.put("store_id", storeId);
                    Jsoup.connect(BASKET_ITEMS_URL)
                            .header("Authorization", UserContext.TOKEN())
                            .header("apikey", UserContext.SECRET())
                            .header("Content-Type", "application/json")
                            .header("Prefer", "return=minimal")
                            .requestBody(newItem.toString())
                            .ignoreContentType(true)
                            .post();
                }
                return true;
            } catch (Exception e) {
                error = "Error: " + e.getMessage();
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
        private final String filter;
        private final LoadCartCallback callback;
        private String error;

        LoadCartItemsTask(String filter, LoadCartCallback callback) {
            this.filter = filter;
            this.callback = callback;
        }

        @Override
        protected List<CartItem> doInBackground(Void... voids) {
            try {
                String basketItemsUrl = BASKET_ITEMS_URL + "?" + filter + "&select=id,count,product_size_id";
                Document basketDoc = Jsoup.connect(basketItemsUrl)
                        .header("Authorization", UserContext.TOKEN())
                        .header("apikey", UserContext.SECRET())
                        .ignoreContentType(true)
                        .get();

                JSONArray basketItemsArray = new JSONArray(basketDoc.body().text());
                List<CartItem> items = new ArrayList<>();

                for (int i = 0; i < basketItemsArray.length(); i++) {
                    JSONObject itemObj = basketItemsArray.getJSONObject(i);
                    if (!itemObj.has("product_size_id")) {
                        continue;
                    }
                    int productSizeId = itemObj.getInt("product_size_id");
                    int count = itemObj.getInt("count");
                    String itemId = itemObj.getString("id");
                    String productSizeUrl = "https://mgxymxiehfsptuubuqfv.supabase.co/rest/v1/product_size?id=eq." + productSizeId + "&select=id,product_id,size_id,count,store_id";
                    Document productSizeDoc = Jsoup.connect(productSizeUrl)
                            .header("Authorization", UserContext.TOKEN())
                            .header("apikey", UserContext.SECRET())
                            .ignoreContentType(true)
                            .get();
                    JSONArray productSizeArray = new JSONArray(productSizeDoc.body().text());
                    JSONObject productSizeObj = productSizeArray.getJSONObject(0);
                    int productId = productSizeObj.getInt("product_id");
                    int sizeId = productSizeObj.getInt("size_id");
                    int availableQuantity = productSizeObj.optInt("count", 0);
                    String productUrl = "https://mgxymxiehfsptuubuqfv.supabase.co/rest/v1/products?id=eq." + productId + "&select=id,name,price,description,category_id,brand_id,shoe_color_id,sole_color_id";
                    Document productDoc = Jsoup.connect(productUrl)
                            .header("Authorization", UserContext.TOKEN())
                            .header("apikey", UserContext.SECRET())
                            .ignoreContentType(true)
                            .get();
                    JSONArray productArray = new JSONArray(productDoc.body().text());
                    Product product = null;
                    if (productArray.length() > 0) {
                        JSONObject productObj = productArray.getJSONObject(0);
                        product = new Product(
                                productObj.getInt("id"),
                                productObj.getString("name"),
                                productObj.getDouble("price"),
                                productObj.getString("description"),
                                productObj.getInt("category_id"),
                                productObj.getInt("brand_id"),
                                productObj.getInt("shoe_color_id"),
                                productObj.getInt("sole_color_id")
                        );
                    }
                    String sizeUrl = "https://mgxymxiehfsptuubuqfv.supabase.co/rest/v1/sizes?id=eq." + sizeId + "&select=value";
                    Document sizeDoc = Jsoup.connect(sizeUrl)
                            .header("Authorization", UserContext.TOKEN())
                            .header("apikey", UserContext.SECRET())
                            .ignoreContentType(true)
                            .get();
                    JSONArray sizeArray = new JSONArray(sizeDoc.body().text());
                    String sizeValue = "Unknown";
                    if (sizeArray.length() > 0) {
                        JSONObject sizeObj = sizeArray.getJSONObject(0);
                        sizeValue = sizeObj.getString("value");
                    }
                    CartItem item = new CartItem(itemId, product, count, sizeValue,
                            availableQuantity, productSizeId);
                    items.add(item);
                }
                return items;
            } catch (Exception e) {
                error = "Error: " + e.getMessage();
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
                        .header("Authorization", UserContext.TOKEN())
                        .header("apikey", UserContext.SECRET())
                        .header("Content-Type", "application/json")
                        .requestBody(update.toString())
                        .ignoreContentType(true)
                        .method(org.jsoup.Connection.Method.PATCH)
                        .execute();
                return true;
            } catch (Exception e) {
                error = "Error: " + e.getMessage();
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
                        .header("Authorization", UserContext.TOKEN())
                        .header("apikey", UserContext.SECRET())
                        .ignoreContentType(true)
                        .method(org.jsoup.Connection.Method.DELETE)
                        .execute();
                return true;
            } catch (Exception e) {
                error = "Error: " + e.getMessage();
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

    private static class LoadSimpleCartItemsTask extends AsyncTask<Void, Void, List<CartItem>> {
        private final String filter;
        private final LoadCartCallback callback;
        private String error;

        LoadSimpleCartItemsTask(String filter, LoadCartCallback callback) {
            this.filter = filter;
            this.callback = callback;
        }

        @Override
        protected List<CartItem> doInBackground(Void... voids) {
            try {
                String url = BASKET_ITEMS_URL + "?" + filter;
                Document doc = Jsoup.connect(url)
                        .header("Authorization", UserContext.TOKEN())
                        .header("apikey", UserContext.SECRET())
                        .ignoreContentType(true)
                        .get();
                JSONArray itemsArray = new JSONArray(doc.body().text());
                List<CartItem> items = new ArrayList<>();
                for (int i = 0; i < itemsArray.length(); i++) {
                    JSONObject itemObj = itemsArray.getJSONObject(i);
                    String id = itemObj.getString("id");
                    int count = itemObj.getInt("count");
                    int productSizeId = 0;
                    if (itemObj.has("product_size_id") && !itemObj.isNull("product_size_id")) {
                        productSizeId = itemObj.getInt("product_size_id");
                    }
                    CartItem item = new CartItem(id, null, count, null, 0, productSizeId);
                    items.add(item);
                }
                return items;
            } catch (Exception e) {
                error = "Error: " + e.getMessage();
                Log.e("CartContext", error, e);
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

    private static class ClearCartTask extends AsyncTask<Void, Void, Boolean> {
        private final String basketId;
        private final UpdateCartCallback callback;
        private String error;

        ClearCartTask(String basketId, UpdateCartCallback callback) {
            this.basketId = basketId;
            this.callback = callback;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                String url = BASKET_URL + "?basket_id=eq." + basketId;
                Connection.Response response = Jsoup.connect(url)
                        .header("Authorization", UserContext.TOKEN())
                        .header("apikey", UserContext.SECRET())
                        .header("Prefer", "return=minimal")
                        .method(Connection.Method.DELETE)
                        .ignoreContentType(true)
                        .execute();
                return response.statusCode() == 204 || response.statusCode() == 200;
            } catch (Exception e) {
                error = e.getMessage();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                callback.onSuccess();
            } else {
                callback.onError(error != null ? error : "Unknown error");
            }
        }
    }
}

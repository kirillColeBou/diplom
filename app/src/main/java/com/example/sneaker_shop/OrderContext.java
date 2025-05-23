package com.example.sneaker_shop;

import android.os.AsyncTask;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderContext {
    private static final String ORDERS_URL = "https://mgxymxiehfsptuubuqfv.supabase.co/rest/v1/orders";
    private static final String ORDER_ITEMS_URL = "https://mgxymxiehfsptuubuqfv.supabase.co/rest/v1/order_items";
    private static final String PRODUCT_SIZE_URL = "https://mgxymxiehfsptuubuqfv.supabase.co/rest/v1/product_size";
    private static final String TOKEN = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im1neHlteGllaGZzcHR1dWJ1cWZ2Iiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc0NTIyNzY0NSwiZXhwIjoyMDYwODAzNjQ1fQ.LNqLc1o8I8eZUxYuFXknXZZhzN5kRh0eggmg5tItiM0";
    private static final String SECRET = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im1neHlteGllaGZzcHR1dWJ1cWZ2Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDUyMjc2NDUsImV4cCI6MjA2MDgwMzY0NX0.QXcy5Dpd4_b58-xfpvPAIgm9U8Pj6w62RW6p7NDUKyQ";

    public interface OrderCallback {
        void onSuccess(long orderId);
        void onError(String error);
    }

    public interface CompleteCallback {
        void onSuccess();
        void onError(String error);
    }

    public static void createOrder(long userId, int storeId, double totalPrice, List<CartItem> items, OrderCallback callback) {
        new CreateOrderTask(userId, storeId, totalPrice, items, callback).execute();
    }

    private static class CreateOrderTask extends AsyncTask<Void, Void, Long> {
        private final long userId;
        private final int storeId;
        private final double totalPrice;
        private final List<CartItem> items;
        private final OrderCallback callback;
        private String error;

        CreateOrderTask(long userId, int storeId, double totalPrice, List<CartItem> items, OrderCallback callback) {
            this.userId = userId;
            this.storeId = storeId;
            this.totalPrice = totalPrice;
            this.items = items;
            this.callback = callback;
        }

        @Override
        protected Long doInBackground(Void... voids) {
            try {
                String orderDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(new Date());
                JSONObject orderJson = new JSONObject();
                orderJson.put("user_uid", userId);
                orderJson.put("order_date", orderDate);
                orderJson.put("status", "В сборке");
                orderJson.put("total_price", totalPrice);
                orderJson.put("store_id", storeId);
                Connection.Response orderResponse = Jsoup.connect(ORDERS_URL)
                        .timeout(10000)
                        .header("Authorization", TOKEN)
                        .header("apikey", SECRET)
                        .header("Content-Type", "application/json")
                        .header("Prefer", "return=representation")
                        .requestBody(orderJson.toString())
                        .ignoreContentType(true)
                        .method(Connection.Method.POST)
                        .execute();
                JSONArray orderArray = new JSONArray(orderResponse.body());
                if (orderArray.length() > 0) {
                    JSONObject orderResult = orderArray.getJSONObject(0);
                    long orderId = orderResult.getLong("id");
                    for (CartItem item : items) {
                        JSONObject itemJson = new JSONObject();
                        itemJson.put("order_id", orderId);
                        itemJson.put("count", item.getCount());
                        itemJson.put("product_size_id", item.getProductSizeId());
                        itemJson.put("total_price_product", item.getProduct().getPrice() * item.getCount());
                        Connection.Response itemResponse = Jsoup.connect(ORDER_ITEMS_URL)
                                .header("Authorization", TOKEN)
                                .header("apikey", SECRET)
                                .header("Content-Type", "application/json")
                                .header("Prefer", "return=minimal")
                                .requestBody(itemJson.toString())
                                .ignoreContentType(true)
                                .method(Connection.Method.POST)
                                .execute();
                        JSONObject updateJson = new JSONObject();
                        updateJson.put("count", item.getAvailableQuantity() - item.getCount());
                        Connection.Response updateResponse = Jsoup.connect(PRODUCT_SIZE_URL + "?id=eq." + item.getProductSizeId())
                                .header("Authorization", TOKEN)
                                .header("apikey", SECRET)
                                .header("Content-Type", "application/json")
                                .header("Prefer", "return=minimal")
                                .requestBody(updateJson.toString())
                                .ignoreContentType(true)
                                .method(Connection.Method.PATCH)
                                .execute();
                    }
                    return orderId;
                }
                return -1L;
            } catch (Exception e) {
                error = "Error: " + e.getMessage();
                Log.e("OrderError", error, e);
                return -1L;
            }
        }

        @Override
        protected void onPostExecute(Long orderId) {
            Log.d("CreateOrderTask", "onPostExecute called with orderId: " + orderId + ", error: " + error);
            if (error != null || orderId == -1L) {
                callback.onError(error != null ? error : "Не удалось создать заказ");
            } else {
                callback.onSuccess(orderId);
            }
        }
    }

    public interface LoadOrdersCallback {
        void onSuccess(List<Order> orders);
        void onError(String error);
    }

    public static void loadUserOrders(long userId, LoadOrdersCallback callback) {
        new LoadOrdersTask(userId, callback).execute();
    }

    private static class LoadOrdersTask extends AsyncTask<Void, Void, List<Order>> {
        private final long userId;
        private final LoadOrdersCallback callback;
        private String error;

        LoadOrdersTask(long userId, LoadOrdersCallback callback) {
            this.userId = userId;
            this.callback = callback;
        }

        @Override
        protected List<Order> doInBackground(Void... voids) {
            List<Order> orders = new ArrayList<>();
            try {
                String filter = "user_uid=eq." + userId + "&select=*,order_items(count,product_size_id)";
                Connection.Response response = Jsoup.connect(ORDERS_URL + "?" + filter)
                        .header("Authorization", TOKEN)
                        .header("apikey", SECRET)
                        .ignoreContentType(true)
                        .method(Connection.Method.GET)
                        .execute();
                JSONArray ordersArray = new JSONArray(response.body());
                for (int i = 0; i < ordersArray.length(); i++) {
                    JSONObject orderJson = ordersArray.getJSONObject(i);
                    long id = orderJson.getLong("id");
                    String date = orderJson.getString("order_date");
                    String status = orderJson.getString("status");
                    double totalPrice = orderJson.getDouble("total_price");
                    int itemCount = 0;
                    if (orderJson.has("order_items")) {
                        JSONArray itemsArray = orderJson.getJSONArray("order_items");
                        for (int j = 0; j < itemsArray.length(); j++) {
                            JSONObject item = itemsArray.getJSONObject(j);
                            itemCount += item.getInt("count");
                        }
                    }
                    orders.add(new Order(id, date, status, totalPrice, itemCount));
                }
                return orders;
            } catch (Exception e) {
                error = "Error: " + e.getMessage();
                Log.e("OrderError", error, e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<Order> orders) {
            if (error != null || orders == null) {
                callback.onError(error != null ? error : "Не удалось загрузить заказы");
            } else {
                callback.onSuccess(orders);
            }
        }
    }
}
package com.example.sneaker_shop;

import android.content.Context;
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
    private static final String PRODUCTS_URL = "https://mgxymxiehfsptuubuqfv.supabase.co/rest/v1/products";
    private static final String SIZES_URL = "https://mgxymxiehfsptuubuqfv.supabase.co/rest/v1/sizes";
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

    public interface LoadOrdersCallback {
        void onSuccess(List<Order> orders);
        void onError(String error);
    }

    public interface LoadOrderItemsCallback {
        void onSuccess(List<CartItem> items, String status);
        void onError(String error);
    }

    public interface UpdateOrderCallback {
        void onSuccess();
        void onError(String error);
    }

    public static void createOrder(Context context, long userId, int storeId, double totalPrice, List<CartItem> items, OrderCallback callback) {
        new CreateOrderTask(context, userId, storeId, totalPrice, items, callback).execute();
    }

    public static void loadUserOrders(long userId, LoadOrdersCallback callback) {
        new LoadOrdersTask(userId, callback).execute();
    }

    public static void loadOrderItems(long orderId, LoadOrderItemsCallback callback) {
        new LoadOrderItemsTask(orderId, callback).execute();
    }

    public static void updateOrderStatus(long orderId, String newStatus, UpdateOrderCallback callback) {
        new UpdateOrderStatusTask(orderId, newStatus, callback).execute();
    }

    public static void cancelOrder(long orderId, UpdateOrderCallback callback) {
        new CancelOrderTask(orderId, callback).execute();
    }

    private static class CreateOrderTask extends AsyncTask<Void, Void, Long> {
        private final long userId;
        private final int storeId;
        private final double totalPrice;
        private final List<CartItem> items;
        private final OrderCallback callback;
        private final Context context;
        private String error;

        CreateOrderTask(Context context, long userId, int storeId, double totalPrice, List<CartItem> items, OrderCallback callback) {
            this.context = context.getApplicationContext();
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
                try {
                    OrderStatusWorkerScheduler.scheduleOrderStatusCheck(context);
                    Log.d("CreateOrderTask", "Scheduled OrderStatusWorker for orderId: " + orderId);
                } catch (Exception e) {
                    Log.e("CreateOrderTask", "Failed to schedule OrderStatusWorker: " + e.getMessage(), e);
                }
            }
        }
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

    private static class LoadOrderItemsTask extends AsyncTask<Void, Void, List<CartItem>> {
        private final long orderId;
        private final LoadOrderItemsCallback callback;
        private String error;
        private String orderStatus;

        LoadOrderItemsTask(long orderId, LoadOrderItemsCallback callback) {
            this.orderId = orderId;
            this.callback = callback;
        }

        @Override
        protected List<CartItem> doInBackground(Void... voids) {
            List<CartItem> items = new ArrayList<>();
            try {
                String orderFilter = "id=eq." + orderId + "&select=status";
                Log.d("LoadOrderItemsTask", "Fetching order status for orderId: " + orderId);
                Connection.Response orderResponse = Jsoup.connect(ORDERS_URL + "?" + orderFilter)
                        .header("Authorization", TOKEN)
                        .header("apikey", SECRET)
                        .ignoreContentType(true)
                        .method(Connection.Method.GET)
                        .execute();
                JSONArray orderArray = new JSONArray(orderResponse.body());
                Log.d("LoadOrderItemsTask", "Order response: " + orderResponse.body());
                if (orderArray.length() == 0) {
                    error = "Заказ не найден";
                    Log.e("LoadOrderItemsTask", error);
                    return null;
                }
                orderStatus = orderArray.getJSONObject(0).getString("status");
                Log.d("LoadOrderItemsTask", "Order status: " + orderStatus);
                String filter = "order_id=eq." + orderId + "&select=count,product_size_id";
                Log.d("LoadOrderItemsTask", "Fetching order items for orderId: " + orderId);
                Connection.Response response = Jsoup.connect(ORDER_ITEMS_URL + "?" + filter)
                        .header("Authorization", TOKEN)
                        .header("apikey", SECRET)
                        .ignoreContentType(true)
                        .method(Connection.Method.GET)
                        .execute();
                JSONArray itemsArray = new JSONArray(response.body());
                Log.d("LoadOrderItemsTask", "Order items response: " + response.body() + ", items count: " + itemsArray.length());
                for (int i = 0; i < itemsArray.length(); i++) {
                    JSONObject itemObj = itemsArray.getJSONObject(i);
                    int count = itemObj.getInt("count");
                    int productSizeId = itemObj.getInt("product_size_id");
                    Log.d("LoadOrderItemsTask", "Processing item: count=" + count + ", productSizeId=" + productSizeId);
                    String productSizeUrl = PRODUCT_SIZE_URL + "?id=eq." + productSizeId + "&select=id,product_id,size_id,count";
                    Connection.Response productSizeResponse = Jsoup.connect(productSizeUrl)
                            .header("Authorization", TOKEN)
                            .header("apikey", SECRET)
                            .ignoreContentType(true)
                            .method(Connection.Method.GET)
                            .execute();
                    JSONArray productSizeArray = new JSONArray(productSizeResponse.body());
                    Log.d("LoadOrderItemsTask", "Product size response: " + productSizeResponse.body());
                    if (productSizeArray.length() == 0) {
                        Log.w("LoadOrderItemsTask", "No product size found for productSizeId: " + productSizeId);
                        continue;
                    }
                    JSONObject productSizeObj = productSizeArray.getJSONObject(0);
                    int productId = productSizeObj.getInt("product_id");
                    int sizeId = productSizeObj.getInt("size_id");
                    int availableQuantity = productSizeObj.getInt("count");
                    String productUrl = PRODUCTS_URL + "?id=eq." + productId + "&select=id,name,price,description,category_id";
                    Connection.Response productResponse = Jsoup.connect(productUrl)
                            .header("Authorization", TOKEN)
                            .header("apikey", SECRET)
                            .ignoreContentType(true)
                            .method(Connection.Method.GET)
                            .execute();
                    JSONArray productArray = new JSONArray(productResponse.body());
                    Log.d("LoadOrderItemsTask", "Product response: " + productResponse.body());
                    Product product = null;
                    if (productArray.length() > 0) {
                        JSONObject productObj = productArray.getJSONObject(0);
                        product = new Product(
                                productObj.getInt("id"),
                                productObj.getString("name"),
                                productObj.getDouble("price"),
                                productObj.getString("description"),
                                productObj.getInt("category_id")
                        );
                    } else {
                        Log.w("LoadOrderItemsTask", "No product found for productId: " + productId);
                        continue;
                    }
                    String sizeUrl = SIZES_URL + "?id=eq." + sizeId + "&select=value";
                    Connection.Response sizeResponse = Jsoup.connect(sizeUrl)
                            .header("Authorization", TOKEN)
                            .header("apikey", SECRET)
                            .ignoreContentType(true)
                            .method(Connection.Method.GET)
                            .execute();
                    JSONArray sizeArray = new JSONArray(sizeResponse.body());
                    Log.d("LoadOrderItemsTask", "Size response: " + sizeResponse.body());
                    String sizeValue = "Unknown";
                    if (sizeArray.length() > 0) {
                        sizeValue = sizeArray.getJSONObject(0).getString("value");
                    }
                    items.add(new CartItem(null, product, count, sizeValue, availableQuantity, productSizeId));
                    Log.d("LoadOrderItemsTask", "Added item: product=" + product.getName() + ", count=" + count + ", size=" + sizeValue);
                }
                Log.d("LoadOrderItemsTask", "Total items loaded: " + items.size());
                return items;
            } catch (Exception e) {
                error = "Error: " + e.getMessage();
                Log.e("OrderError", error, e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<CartItem> items) {
            Log.d("LoadOrderItemsTask", "onPostExecute: items=" + (items != null ? items.size() : "null") + ", error=" + error);
            if (error != null || items == null) {
                callback.onError(error != null ? error : "Не удалось загрузить элементы заказа");
            } else {
                callback.onSuccess(items, orderStatus);
            }
        }
    }

    private static class UpdateOrderStatusTask extends AsyncTask<Void, Void, Boolean> {
        private final long orderId;
        private final String newStatus;
        private final UpdateOrderCallback callback;
        private String error;

        UpdateOrderStatusTask(long orderId, String newStatus, UpdateOrderCallback callback) {
            this.orderId = orderId;
            this.newStatus = newStatus;
            this.callback = callback;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                JSONObject updateJson = new JSONObject();
                updateJson.put("status", newStatus);
                Connection.Response response = Jsoup.connect(ORDERS_URL + "?id=eq." + orderId)
                        .header("Authorization", TOKEN)
                        .header("apikey", SECRET)
                        .header("Content-Type", "application/json")
                        .header("Prefer", "return=minimal")
                        .requestBody(updateJson.toString())
                        .ignoreContentType(true)
                        .method(Connection.Method.PATCH)
                        .execute();
                return response.statusCode() == 204 || response.statusCode() == 200;
            } catch (Exception e) {
                error = "Error: " + e.getMessage();
                Log.e("OrderError", error, e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (error != null || !success) {
                callback.onError(error != null ? error : "Не удалось обновить статус заказа");
            } else {
                callback.onSuccess();
            }
        }
    }

    private static class CancelOrderTask extends AsyncTask<Void, Void, Boolean> {
        private final long orderId;
        private final UpdateOrderCallback callback;
        private String error;

        CancelOrderTask(long orderId, UpdateOrderCallback callback) {
            this.orderId = orderId;
            this.callback = callback;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                String filter = "order_id=eq." + orderId + "&select=count,product_size_id";
                Connection.Response itemsResponse = Jsoup.connect(ORDER_ITEMS_URL + "?" + filter)
                        .header("Authorization", TOKEN)
                        .header("apikey", SECRET)
                        .ignoreContentType(true)
                        .method(Connection.Method.GET)
                        .execute();
                JSONArray itemsArray = new JSONArray(itemsResponse.body());
                for (int i = 0; i < itemsArray.length(); i++) {
                    JSONObject itemObj = itemsArray.getJSONObject(i);
                    int count = itemObj.getInt("count");
                    int productSizeId = itemObj.getInt("product_size_id");
                    String productSizeUrl = PRODUCT_SIZE_URL + "?id=eq." + productSizeId + "&select=count";
                    Connection.Response productSizeResponse = Jsoup.connect(productSizeUrl)
                            .header("Authorization", TOKEN)
                            .header("apikey", SECRET)
                            .ignoreContentType(true)
                            .method(Connection.Method.GET)
                            .execute();
                    JSONArray productSizeArray = new JSONArray(productSizeResponse.body());
                    if (productSizeArray.length() == 0) continue;
                    int currentCount = productSizeArray.getJSONObject(0).getInt("count");
                    JSONObject updateJson = new JSONObject();
                    updateJson.put("count", currentCount + count);
                    Connection.Response updateResponse = Jsoup.connect(PRODUCT_SIZE_URL + "?id=eq." + productSizeId)
                            .header("Authorization", TOKEN)
                            .header("apikey", SECRET)
                            .header("Content-Type", "application/json")
                            .header("Prefer", "return=minimal")
                            .requestBody(updateJson.toString())
                            .ignoreContentType(true)
                            .method(Connection.Method.PATCH)
                            .execute();
                }
                JSONObject updateJson = new JSONObject();
                updateJson.put("status", "Отменён");
                Connection.Response response = Jsoup.connect(ORDERS_URL + "?id=eq." + orderId)
                        .header("Authorization", TOKEN)
                        .header("apikey", SECRET)
                        .header("Content-Type", "application/json")
                        .header("Prefer", "return=minimal")
                        .requestBody(updateJson.toString())
                        .ignoreContentType(true)
                        .method(Connection.Method.PATCH)
                        .execute();
                return response.statusCode() == 204 || response.statusCode() == 200;
            } catch (Exception e) {
                error = "Error: " + e.getMessage();
                Log.e("OrderError", error, e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (error != null || !success) {
                callback.onError(error != null ? error : "Не удалось отменить заказ");
            } else {
                callback.onSuccess();
            }
        }
    }
}
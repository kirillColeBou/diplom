package com.example.sneaker_shop;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import java.util.HashSet;
import java.util.Set;

public class OrderCheckWorker extends Worker {
    private static final String CHANNEL_ID = "order_notifications";
    private static final String CHANNEL_NAME = "Order Status Updates";
    private static final String ORDERS_URL = "https://mgxymxiehfsptuubuqfv.supabase.co/rest/v1/orders";
    private static final String TOKEN = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im1neHlteGllaGZzcHR1dWJ1cWZ2Iiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc0NTIyNzY0NSwiZXhwIjoyMDYwODAzNjQ1fQ.LNqLc1o8I8eZUxYuFXknXZZhzN5kRh0eggmg5tItiM0";
    private static final String SECRET = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im1neHlteGllaGZzcHR1dWJ1cWZ2Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDUyMjc2NDUsImV4cCI6MjA2MDgwMzY0NX0.QXcy5Dpd4_b58-xfpvPAIgm9U8Pj6w62RW6p7NDUKyQ";

    public OrderCheckWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        long userId = AuthUtils.getCurrentUserId(getApplicationContext());
        Set<String> notifiedOrders = getNotifiedOrders();
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
                String orderId = String.valueOf(orderJson.getLong("id"));
                String status = orderJson.getString("status");
                String storeName = "Неизвестный магазин";
                if ("Ожидает получения".equals(status) && !notifiedOrders.contains(orderId)) {
                    sendNotification(orderId, storeName);
                    notifiedOrders.add(orderId);
                    saveNotifiedOrders(notifiedOrders);
                }
            }
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }

    private void sendNotification(String orderId, String storeName) {
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }
        Intent intent = new Intent(getApplicationContext(), OrderActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(),
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.notification)
                .setContentTitle("Заказ #" + orderId)
                .setContentText("Ваш заказ ожидает получения в магазине " + storeName)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private Set<String> getNotifiedOrders() {
        return new HashSet<>(getApplicationContext()
                .getSharedPreferences("SneakerShopPrefs", Context.MODE_PRIVATE)
                .getStringSet("notified_orders", new HashSet<>()));
    }

    private void saveNotifiedOrders(Set<String> notifiedOrders) {
        getApplicationContext()
                .getSharedPreferences("SneakerShopPrefs", Context.MODE_PRIVATE)
                .edit()
                .putStringSet("notified_orders", notifiedOrders)
                .apply();
    }
}
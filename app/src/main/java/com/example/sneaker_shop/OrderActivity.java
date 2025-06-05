package com.example.sneaker_shop;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import java.util.ArrayList;
import java.util.List;

public class OrderActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private OrderAdapter adapter;
    private List<Order> orders = new ArrayList<>();
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout emptyOrderLayout;
    private long currentUserId;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1003;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.order_activity);
        currentUserId = AuthUtils.getCurrentUserId(this);
        recyclerView = findViewById(R.id.recycler_view_order);
        emptyOrderLayout = findViewById(R.id.empty_order_item);
        adapter = new OrderAdapter(orders);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        swipeRefreshLayout.setOnRefreshListener(this::loadOrders);
        checkNotificationPermission();
        if (getIntent().getBooleanExtra("refreshOrders", false)) {
            loadOrders();
        } else {
            loadOrders();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent.getBooleanExtra("refreshOrders", false)) {
            loadOrders();
        }
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE);
            } else {
                Log.d("OrderActivity", "Notification permission already granted");
            }
        } else {
            Log.d("OrderActivity", "Notification permission not required for this Android version");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("OrderActivity", "Notification permission granted");
            } else {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)) {
                    Toast.makeText(this, "Разрешение на уведомления отклонено. Включите в настройках приложения.", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Разрешение на уведомления отклонено", Toast.LENGTH_SHORT).show();
                    Log.d("OrderActivity", "Notification permission denied");
                }
            }
        }
    }

    private void loadOrders() {
        swipeRefreshLayout.setRefreshing(true);
        OrderContext.loadUserOrders(currentUserId, new OrderContext.LoadOrdersCallback() {
            @Override
            public void onSuccess(List<Order> loadedOrders) {
                runOnUiThread(() -> {
                    swipeRefreshLayout.setRefreshing(false);
                    adapter.updateOrders(loadedOrders);
                    if (loadedOrders.isEmpty()) {
                        recyclerView.setVisibility(View.GONE);
                        emptyOrderLayout.setVisibility(View.VISIBLE);
                    } else {
                        recyclerView.setVisibility(View.VISIBLE);
                        emptyOrderLayout.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    swipeRefreshLayout.setRefreshing(false);
                    recyclerView.setVisibility(View.GONE);
                    emptyOrderLayout.setVisibility(View.VISIBLE);
                    Toast.makeText(OrderActivity.this,
                            "Ошибка загрузки заказов: " + error,
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    public void onBack(View view) {
        startActivity(new Intent(this, MenuActivity.class));
        overridePendingTransition(0, 0);
        finish();
    }
}
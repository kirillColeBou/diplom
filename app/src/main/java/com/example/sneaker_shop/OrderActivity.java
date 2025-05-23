package com.example.sneaker_shop;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
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
    private ProgressBar progressBar;
    private long currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.order_activity);
        currentUserId = AuthUtils.getCurrentUserId(this);
        recyclerView = findViewById(R.id.recycler_view_order);
        adapter = new OrderAdapter(orders);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        progressBar = findViewById(R.id.progressBar);
        swipeRefreshLayout.setOnRefreshListener(this::loadOrders);
        loadOrders();
    }

    private void loadOrders() {
        progressBar.setVisibility(View.VISIBLE);
        OrderContext.loadUserOrders(currentUserId, new OrderContext.LoadOrdersCallback() {
            @Override
            public void onSuccess(List<Order> loadedOrders) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                    adapter.updateOrders(loadedOrders);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
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
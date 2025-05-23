package com.example.sneaker_shop;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import java.util.ArrayList;
import java.util.List;

public class OrderInfoActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private OrderItemsAdapter adapter;
    private List<CartItem> orderItems;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Button btnOrderReceived;
    private TextView cancelOrderText;
    private long orderId;
    private String orderStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.order_info_activity);
        orderId = getIntent().getLongExtra("orderId", -1);
        if (orderId == -1) {
            Toast.makeText(this, "Ошибка: заказ не найден", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        recyclerView = findViewById(R.id.recycler_view_order_info);
        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        btnOrderReceived = findViewById(R.id.btn_order_received);
        cancelOrderText = findViewById(R.id.cancel_order_text);
        orderItems = new ArrayList<>();
        adapter = new OrderItemsAdapter(orderItems, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        swipeRefreshLayout.setOnRefreshListener(this::loadOrderItems);
        cancelOrderText.setOnClickListener(v -> cancelOrder());
        loadOrderItems();
    }

    private void loadOrderItems() {
        swipeRefreshLayout.setRefreshing(true);
        OrderContext.loadOrderItems(orderId, new OrderContext.LoadOrderItemsCallback() {
            @Override
            public void onSuccess(List<CartItem> items, String status) {
                runOnUiThread(() -> {
                    orderStatus = status;
                    orderItems.clear();
                    orderItems.addAll(items);
                    adapter.updateItems(orderItems);
                    updateUI();
                    swipeRefreshLayout.setRefreshing(false);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(OrderInfoActivity.this, "Ошибка: " + error, Toast.LENGTH_SHORT).show();
                    swipeRefreshLayout.setRefreshing(false);
                });
            }
        });
    }

    private void updateUI() {
        TextView orderNumberText = findViewById(R.id.order_number_text);
        orderNumberText.setText(String.format("Заказ №%d", orderId));
        btnOrderReceived.setVisibility("Ожидает получения".equals(orderStatus) ? View.VISIBLE : View.GONE);
        cancelOrderText.setVisibility("Отменён".equals(orderStatus) || "Получен".equals(orderStatus) ? View.GONE : View.VISIBLE);
        cancelOrderText.setEnabled(!"Отменён".equals(orderStatus) && !"Получен".equals(orderStatus));
    }

    public void onBack(View view) {
        startActivity(new Intent(this, OrderActivity.class));
        overridePendingTransition(0, 0);
        finish();
    }

    public void onOrderReceived(View view) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Обновление статуса...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        OrderContext.updateOrderStatus(orderId, "Получен", new OrderContext.UpdateOrderCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    orderStatus = "Получен";
                    updateUI();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(OrderInfoActivity.this, "Ошибка: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void cancelOrder() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Отмена заказа...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        OrderContext.cancelOrder(orderId, new OrderContext.UpdateOrderCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Intent intent = new Intent(OrderInfoActivity.this, OrderActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    intent.putExtra("refreshOrders", true);
                    startActivity(intent);
                    finish();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(OrderInfoActivity.this, "Ошибка: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
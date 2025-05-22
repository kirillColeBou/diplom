package com.example.sneaker_shop;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import java.util.ArrayList;
import java.util.List;

public class CartActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private CartAdapter adapter;
    private List<CartItem> cartItems;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout emptyCartLayout;
    private long currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cart_activity);
        currentUserId = AuthUtils.getCurrentUserId(this);
        recyclerView = findViewById(R.id.recycler_view_cart);
        emptyCartLayout = findViewById(R.id.empty_cart);
        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        cartItems = new ArrayList<>();
        adapter = new CartAdapter(cartItems, this);
        recyclerView.setAdapter(adapter);
        swipeRefreshLayout.setOnRefreshListener(this::loadCartItems);
        loadCartItems();
    }

    private void loadCartItems() {
        swipeRefreshLayout.setRefreshing(true);
        int selectedStoreId = PreferencesHelper.getSelectedStoreId(this);
        if (selectedStoreId == -1) {
            Toast.makeText(this, "Пожалуйста, выберите магазин", Toast.LENGTH_SHORT).show();
            swipeRefreshLayout.setRefreshing(false);
            checkEmptyState();
            return;
        }
        CartContext.getUserBasket(currentUserId, new CartContext.BasketCallback() {
            @Override
            public void onSuccess(String basketId) {
                String filter = "basket_id=eq." + basketId + "&store_id=eq." + selectedStoreId;
                CartContext.loadCartItems(filter, new CartContext.LoadCartCallback() {
                    @Override
                    public void onSuccess(List<CartItem> items) {
                        runOnUiThread(() -> {
                            cartItems.clear();
                            if (items != null) {
                                cartItems.addAll(items);
                            }
                            adapter.updateItems(cartItems);
                            checkEmptyState();
                            swipeRefreshLayout.setRefreshing(false);
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Toast.makeText(CartActivity.this,
                                    "Ошибка загрузки корзины: " + error,
                                    Toast.LENGTH_SHORT).show();
                            cartItems.clear();
                            adapter.updateItems(cartItems);
                            checkEmptyState();
                            swipeRefreshLayout.setRefreshing(false);
                        });
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(CartActivity.this,
                            "Ошибка: " + error,
                            Toast.LENGTH_SHORT).show();
                    cartItems.clear();
                    adapter.updateItems(cartItems);
                    checkEmptyState();
                    swipeRefreshLayout.setRefreshing(false);
                });
            }
        });
    }

    public void checkEmptyState() {
        if (cartItems == null || cartItems.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyCartLayout.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyCartLayout.setVisibility(View.GONE);
            adapter.notifyDataSetChanged();
        }
    }

    public void onHome(View view) {
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(0, 0);
        finish();
    }

    public void onMenu(View view) {
        startActivity(new Intent(this, MenuActivity.class));
        overridePendingTransition(0, 0);
        finish();
    }

    public void onFavorite(View view) {
        startActivity(new Intent(this, FavoriteActivity.class));
        overridePendingTransition(0, 0);
        finish();
    }

    public void onPerson(View view) {
        startActivity(new Intent(this, PersonActivity.class));
        overridePendingTransition(0, 0);
        finish();
    }
}
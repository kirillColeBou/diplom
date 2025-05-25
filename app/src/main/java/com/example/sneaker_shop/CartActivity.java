package com.example.sneaker_shop;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import java.util.ArrayList;
import java.util.List;

public class CartActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private CartAdapter adapter;
    public List<CartItem> cartItems;
    public SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout emptyCartLayout;
    private LinearLayout createOrderLayout;
    private TextView totalPriceText;
    private TextView addressText;
    private long currentUserId;
    private boolean isRefreshing = false;
    private AlertDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cart_activity);
        currentUserId = AuthUtils.getCurrentUserId(this);
        recyclerView = findViewById(R.id.recycler_view_cart);
        emptyCartLayout = findViewById(R.id.empty_cart);
        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        createOrderLayout = findViewById(R.id.create_order_layout);
        totalPriceText = findViewById(R.id.total_price);
        addressText = findViewById(R.id.address_receiving);
        cartItems = new ArrayList<>();
        adapter = new CartAdapter(cartItems, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        int bottomMargin = (int) (10 * getResources().getDisplayMetrics().density);
        recyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                                       @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                int position = parent.getChildAdapterPosition(view);
                int itemCount = parent.getAdapter() != null ? parent.getAdapter().getItemCount() : 0;
                if (position == itemCount - 1) {
                    outRect.bottom = bottomMargin;
                }
            }
        });
        swipeRefreshLayout.setOnRefreshListener(this::loadCartItems);
        loadCartItems();
    }

    private void loadCartItems() {
        if (isRefreshing) return;
        isRefreshing = true;
        swipeRefreshLayout.setRefreshing(true);
        int selectedStoreId = PreferencesHelper.getSelectedStoreId(this);
        if (selectedStoreId == -1) {
            Toast.makeText(this, "Пожалуйста, выберите магазин", Toast.LENGTH_SHORT).show();
            swipeRefreshLayout.setRefreshing(false);
            isRefreshing = false;
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
                            updateOrderLayout();
                            checkEmptyState();
                            swipeRefreshLayout.setRefreshing(false);
                            isRefreshing = false;
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
                            isRefreshing = false;
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
                    isRefreshing = false;
                });
            }
        });
    }

    public void checkEmptyState() {
        runOnUiThread(() -> {
            if (cartItems == null || cartItems.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                emptyCartLayout.setVisibility(View.VISIBLE);
                createOrderLayout.setVisibility(View.GONE);
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                emptyCartLayout.setVisibility(View.GONE);
                createOrderLayout.setVisibility(View.VISIBLE);
                updateOrderLayout();
            }
        });
    }

    public void updateOrderLayout() {
        runOnUiThread(() -> {
            double totalPrice = 0;
            for (CartItem item : cartItems) {
                totalPrice += item.getProduct().getPrice() * item.getCount();
            }
            totalPriceText.setText(String.format("%d ₽", (int) totalPrice));
            String storeAddress = PreferencesHelper.getSelectedStoreAddress(this);
            addressText.setText(storeAddress != null ? storeAddress : "Адрес не выбран");
        });
    }

    public void onCreateOrder(View view) {
        if (cartItems == null || cartItems.isEmpty()) {
            Toast.makeText(this, "Корзина пуста", Toast.LENGTH_SHORT).show();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.custom_progress_bar_dialog, null);
        builder.setView(dialogView);
        progressDialog = builder.create();
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();
        int storeId = PreferencesHelper.getSelectedStoreId(this);
        if (storeId == -1) {
            progressDialog.dismiss();
            Toast.makeText(this, "Выберите магазин", Toast.LENGTH_SHORT).show();
            return;
        }
        double totalPrice = 0;
        for (CartItem item : cartItems) {
            totalPrice += item.getProduct().getPrice() * item.getCount();
        }
        OrderContext.createOrder(this, currentUserId, storeId, totalPrice, cartItems,
                new OrderContext.OrderCallback() {
                    @Override
                    public void onSuccess(long orderId) {
                        runOnUiThread(() -> {
                            if (progressDialog != null && progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                            showOrderSuccessDialog(orderId);
                            clearCartAfterOrder();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            if (progressDialog != null && progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                            Toast.makeText(CartActivity.this,
                                    "Ошибка оформления заказа: " + error,
                                    Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }

    private void showOrderSuccessDialog(long orderId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.alert_dialog_create_order, null);
        builder.setView(dialogView);
        TextView messageText = dialogView.findViewById(R.id.message);
        String storeAddress = PreferencesHelper.getSelectedStoreAddress(this);
        messageText.setText(String.format("Заказ №%d можно будет получить в магазине %s",
                orderId, storeAddress != null ? storeAddress : "по выбранному адресу"));
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        LinearLayout dialogLayout = dialogView.findViewById(android.R.id.content);
        if (dialogLayout == null) {
            dialogLayout = (LinearLayout) dialogView;
        }
        dialogLayout.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void clearCartAfterOrder() {
        cartItems.clear();
        adapter.updateItems(cartItems);
        updateOrderLayout();
        checkEmptyState();
        CartContext.getUserBasket(currentUserId, new CartContext.BasketCallback() {
            @Override
            public void onSuccess(String basketId) {
                String filter = "basket_id=eq." + basketId;
                CartContext.loadSimpleCartItems(filter, new CartContext.LoadCartCallback() {
                    @Override
                    public void onSuccess(List<CartItem> items) {
                        if (items != null && !items.isEmpty()) {
                            for (CartItem item : items) {
                                CartContext.removeFromCart(item.getId(), new CartContext.UpdateCartCallback() {
                                    @Override
                                    public void onSuccess() {}

                                    @Override
                                    public void onError(String error) {}
                                });
                            }
                        }
                    }

                    @Override
                    public void onError(String error) {}
                });
            }

            @Override
            public void onError(String error) {
                Toast.makeText(CartActivity.this, "Ошибка: " + error, Toast.LENGTH_SHORT).show();
            }
        });
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
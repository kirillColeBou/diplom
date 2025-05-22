package com.example.sneaker_shop;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {
    private static final int MAX_QUANTITY = 10;
    private final Context context;
    private List<CartItem> cartItems;

    public CartAdapter(List<CartItem> cartItems, Context context) {
        this.context = context;
        this.cartItems = cartItems;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.cart_item, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = cartItems.get(position);
        holder.productName.setText(item.getProduct().getName());
        holder.productPrice.setText(String.format("%d ₽", (int) item.getProduct().getPrice()));
        holder.productCount.setText(String.valueOf(item.getCount()));
        holder.productSize.setText("Размер: " + item.getSize());
        int maxAllowed = Math.min(MAX_QUANTITY, item.getAvailableQuantity());
        updateButtonStates(holder, item.getCount(), maxAllowed);
        holder.btnMinus.setOnClickListener(v -> {
            if (item.getCount() > 1) {
                updateItemCount(item, -1, position);
            }
        });
        holder.btnPlus.setOnClickListener(v -> {
            if (item.getCount() < maxAllowed) {
                updateItemCount(item, 1, position);
            }
        });
        holder.btnDelete.setOnClickListener(v -> removeItem(item, position));
    }

    @Override
    public int getItemCount() {
        return cartItems != null ? cartItems.size() : 0;
    }

    private void updateButtonStates(CartViewHolder holder, int currentCount, int maxAllowed) {
        holder.btnMinus.setEnabled(currentCount > 1);
        holder.btnMinus.setImageResource(currentCount <= 1 ? R.drawable.minus : R.drawable.minus_select);
        holder.btnPlus.setEnabled(currentCount < maxAllowed);
        holder.btnPlus.setImageResource(currentCount >= maxAllowed ? R.drawable.plus : R.drawable.plus_select);
    }

    private void updateItemCount(CartItem item, int change, int position) {
        int newCount = item.getCount() + change;
        CartContext.updateCartItem(item.getId(), newCount, new CartContext.UpdateCartCallback() {
            @Override
            public void onSuccess() {
                item.setCount(newCount);
                notifyItemChanged(position);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(context, "Ошибка: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void removeItem(CartItem item, int position) {
        CartContext.removeFromCart(item.getId(), new CartContext.UpdateCartCallback() {
            @Override
            public void onSuccess() {
                cartItems.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, cartItems.size());
                if (context instanceof CartActivity) {
                    ((CartActivity) context).checkEmptyState();
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(context, "Ошибка: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void updateItems(List<CartItem> newItems) {
        if (newItems == null) {
            cartItems = new ArrayList<>();
        } else {
            cartItems = new ArrayList<>(newItems);
        }
        notifyDataSetChanged();
    }

    static class CartViewHolder extends RecyclerView.ViewHolder {
        TextView productName, productPrice, productCount, productSize;
        ImageView btnMinus, btnPlus, btnDelete;

        CartViewHolder(@NonNull View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.product_name);
            productPrice = itemView.findViewById(R.id.product_price);
            productCount = itemView.findViewById(R.id.count_text);
            productSize = itemView.findViewById(R.id.product_size);
            btnMinus = itemView.findViewById(R.id.minus);
            btnPlus = itemView.findViewById(R.id.plus);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
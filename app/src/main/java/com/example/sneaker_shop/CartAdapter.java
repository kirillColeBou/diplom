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
import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {
    private static final int MAX_QUANTITY = 10;
    private List<CartItem> cartItems;
    private final Context context;

    public CartAdapter(List<CartItem> cartItems, Context context) {
        this.cartItems = cartItems;
        this.context = context;
    }

    public void updateItems(List<CartItem> newItems) {
        this.cartItems = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.cart_item, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = cartItems.get(position);
        holder.productName.setText(item.getProduct().getName());
        holder.productPrice.setText(String.format("%d ₽", (int) item.getProduct().getPrice()));
        holder.productCount.setText(String.valueOf(item.getCount()));
        updateButtonStates(holder, item.getCount());
        holder.btnMinus.setOnClickListener(v -> {
            if (item.getCount() > 1) {
                updateItemCount(item, -1, position);
            }
        });
        holder.btnPlus.setOnClickListener(v -> {
            if (item.getCount() < MAX_QUANTITY) {
                updateItemCount(item, 1, position);
            }
        });
        holder.btnDelete.setOnClickListener(v -> {
            removeItem(item, position);
        });
    }

    private void updateButtonStates(CartViewHolder holder, int currentCount) {
        if (currentCount <= 1) {
            holder.btnMinus.setImageResource(R.drawable.minus);
            holder.btnMinus.setEnabled(false);
        } else {
            holder.btnMinus.setImageResource(R.drawable.minus_select);
            holder.btnMinus.setEnabled(true);
        }
        if (currentCount >= MAX_QUANTITY) {
            holder.btnPlus.setImageResource(R.drawable.plus);
            holder.btnPlus.setEnabled(false);
        } else {
            holder.btnPlus.setImageResource(R.drawable.plus_select);
            holder.btnPlus.setEnabled(true);
        }
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
                if (cartItems.isEmpty()) {
                    ((CartActivity) context).checkEmptyState();
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(context, "Ошибка: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return cartItems != null ? cartItems.size() : 0;
    }

    static class CartViewHolder extends RecyclerView.ViewHolder {
        TextView productName;
        TextView productPrice;
        ImageView btnMinus;
        TextView productCount;
        ImageView btnPlus;
        ImageView btnDelete;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.product_name);
            productPrice = itemView.findViewById(R.id.product_price);
            btnMinus = itemView.findViewById(R.id.minus);
            productCount = itemView.findViewById(R.id.count_text);
            btnPlus = itemView.findViewById(R.id.plus);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}

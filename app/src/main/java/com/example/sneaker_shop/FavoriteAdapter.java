package com.example.sneaker_shop;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class FavoriteAdapter extends RecyclerView.Adapter<FavoriteAdapter.FavoriteViewHolder> {
    private final List<Product> favoriteProducts;
    private final Context context;
    private OnMoreClickListener moreClickListener;
    private OnAddToCartClickListener cartClickListener;

    public interface OnMoreClickListener {
        void onMoreClick(int position);
    }

    public interface OnAddToCartClickListener {
        void onAddToCartClick(Product product);
    }

    public FavoriteAdapter(List<Product> favoriteProducts, Context context, OnMoreClickListener listener) {
        this.favoriteProducts = favoriteProducts;
        this.context = context;
        this.moreClickListener = listener;
    }

    public void setOnAddToCartClickListener(OnAddToCartClickListener listener) {
        this.cartClickListener = listener;
    }

    @NonNull
    @Override
    public FavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.product_item, parent, false);
        return new FavoriteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteViewHolder holder, int position) {
        Product product = favoriteProducts.get(position);
        holder.nameProduct.setText(product.getName());
        holder.priceProduct.setText(String.format("%d ₽", (int) product.getPrice()));
        holder.favoriteIcon.setVisibility(View.GONE);
        holder.moreFavorite.setVisibility(View.VISIBLE);
        holder.moreFavorite.setOnClickListener(v -> {
            if (moreClickListener != null) {
                moreClickListener.onMoreClick(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return favoriteProducts.size();
    }

    static class FavoriteViewHolder extends RecyclerView.ViewHolder {
        ImageView imageProduct;
        TextView nameProduct;
        TextView priceProduct;
        ImageView favoriteIcon;
        LinearLayout moreFavorite;

        public FavoriteViewHolder(@NonNull View itemView) {
            super(itemView);
            imageProduct = itemView.findViewById(R.id.iamge_product);
            nameProduct = itemView.findViewById(R.id.name_product);
            priceProduct = itemView.findViewById(R.id.price_product);
            favoriteIcon = itemView.findViewById(R.id.favorite_icon);
            moreFavorite = itemView.findViewById(R.id.more_favorite);
        }
    }
}
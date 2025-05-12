package com.example.sneaker_shop;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
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
    private OnItemClickListener itemClickListener;

    public interface OnItemClickListener {
        void onItemClick(Product product);
    }

    public interface OnMoreClickListener {
        void onMoreClick(int position);
    }

    public FavoriteAdapter(List<Product> favoriteProducts, Context context, OnMoreClickListener moreListener) {
        this.favoriteProducts = favoriteProducts;
        this.context = context;
        this.moreClickListener = moreListener;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
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
        ImageContext.loadImagesForProduct(product.getId(), new ImageContext.ImagesCallback() {
            @Override
            public void onSuccess(List<String> images) {
                if (images != null && !images.isEmpty()) {
                    try {
                        String base64Image = images.get(0).split(",")[1];
                        byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        holder.imageProduct.setImageBitmap(decodedByte);
                    } catch (Exception e) {
                        Log.e("ProductAdapter", "Error loading image", e);
                        holder.imageProduct.setImageResource(R.drawable.nike_air_force);
                    }
                } else {
                    holder.imageProduct.setImageResource(R.drawable.nike_air_force);
                }
            }

            @Override
            public void onError(String error) {
                Log.e("ProductAdapter", "Error loading images: " + error);
                holder.imageProduct.setImageResource(R.drawable.nike_air_force);
            }
        });
        holder.favoriteIcon.setVisibility(View.GONE);
        holder.moreFavorite.setVisibility(View.VISIBLE);
        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null) {
                itemClickListener.onItemClick(product);
            }
        });
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
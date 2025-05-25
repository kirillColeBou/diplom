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
        loadProductImage(product.getId(), holder.imageProduct);
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

    private void loadProductImage(int productId, ImageView imageView) {
        String cacheKey = "product_" + productId + "_0";
        ImageCacheManager cacheManager = ImageCacheManager.getInstance(context);
        Bitmap cachedBitmap = cacheManager.getBitmapFromMemoryCache(cacheKey);
        if (cachedBitmap != null) {
            imageView.setImageBitmap(cachedBitmap);
            Log.d("FavoriteAdapter", "Изображение загружено из кэша памяти: " + cacheKey);
            return;
        }
        cachedBitmap = cacheManager.getBitmapFromDiskCache(cacheKey);
        if (cachedBitmap != null) {
            cacheManager.addBitmapToMemoryCache(cacheKey, cachedBitmap);
            imageView.setImageBitmap(cachedBitmap);
            Log.d("FavoriteAdapter", "Изображение загружено из кэша на диске: " + cacheKey);
            return;
        }
        ImageContext.loadImagesForProduct(context, productId, new ImageContext.ImagesCallback() {
            @Override
            public void onSuccess(List<String> images) {
                if (images != null && !images.isEmpty() && !images.get(0).isEmpty()) {
                    try {
                        String base64Image = images.get(0).split(",")[1];
                        byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        imageView.setImageBitmap(decodedByte);
                        cacheManager.addBitmapToMemoryCache(cacheKey, decodedByte);
                        cacheManager.addBitmapToDiskCache(cacheKey, decodedByte);
                        Log.d("FavoriteAdapter", "Изображение загружено из сети и закэшировано: " + cacheKey);
                    } catch (Exception e) {
                        Log.e("FavoriteAdapter", "Ошибка декодирования изображения", e);
                        imageView.setImageResource(R.drawable.nike_air_force);
                    }
                } else {
                    imageView.setImageResource(R.drawable.nike_air_force);
                }
            }

            @Override
            public void onError(String error) {
                Log.e("FavoriteAdapter", "Ошибка загрузки изображений: " + error);
                imageView.setImageResource(R.drawable.nike_air_force);
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
            imageProduct = itemView.findViewById(R.id.image_product);
            nameProduct = itemView.findViewById(R.id.name_product);
            priceProduct = itemView.findViewById(R.id.price_product);
            favoriteIcon = itemView.findViewById(R.id.favorite_icon);
            moreFavorite = itemView.findViewById(R.id.more_favorite);
        }
    }
}
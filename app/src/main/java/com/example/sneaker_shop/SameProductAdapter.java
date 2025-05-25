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
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class SameProductAdapter extends RecyclerView.Adapter<SameProductAdapter.ViewHolder> {
    private List<Product> products;
    private int selectedPosition = 0;
    private Context context;
    private OnItemClickListener itemClickListener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public SameProductAdapter(List<Product> products, Context context) {
        this.products = products;
        this.context = context;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }

    public void setSelectedPosition(int position) {
        this.selectedPosition = position;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.same_sneaker_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (products == null || products.isEmpty()) {
            return;
        }
        Product product = products.get(position);
        holder.container.setBackgroundResource(
                position == selectedPosition ?
                        R.drawable.background_same_sneaker_select :
                        R.drawable.background_same_sneaker
        );
        loadProductImage(product.getId(), holder.imageSneaker);
        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null) {
                v.animate()
                        .scaleX(0.95f)
                        .scaleY(0.95f)
                        .setDuration(100)
                        .withEndAction(() -> {
                            v.animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(100)
                                    .start();
                            itemClickListener.onItemClick(position);
                        })
                        .start();
            }
        });
    }

    private void loadProductImage(int productId, ImageView imageView) {
        String cacheKey = "product_" + productId + "_0";
        ImageCacheManager cacheManager = ImageCacheManager.getInstance(context);
        Bitmap cachedBitmap = cacheManager.getBitmapFromMemoryCache(cacheKey);
        if (cachedBitmap != null) {
            imageView.setImageBitmap(cachedBitmap);
            Log.d("SameProductAdapter", "Изображение загружено из кэша памяти: " + cacheKey);
            return;
        }
        cachedBitmap = cacheManager.getBitmapFromDiskCache(cacheKey);
        if (cachedBitmap != null) {
            cacheManager.addBitmapToMemoryCache(cacheKey, cachedBitmap);
            imageView.setImageBitmap(cachedBitmap);
            Log.d("SameProductAdapter", "Изображение загружено из кэша на диске: " + cacheKey);
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
                        // Кэшируем Bitmap
                        cacheManager.addBitmapToMemoryCache(cacheKey, decodedByte);
                        cacheManager.addBitmapToDiskCache(cacheKey, decodedByte);
                        Log.d("SameProductAdapter", "Изображение загружено из сети и закэшировано: " + cacheKey);
                    } catch (Exception e) {
                        Log.e("SameProductAdapter", "Ошибка декодирования изображения", e);
                        imageView.setImageResource(R.drawable.nike_air_force);
                    }
                } else {
                    imageView.setImageResource(R.drawable.nike_air_force);
                }
            }

            @Override
            public void onError(String error) {
                Log.e("SameProductAdapter", "Ошибка загрузки изображений: " + error);
                imageView.setImageResource(R.drawable.nike_air_force);
            }
        });
    }

    public void updateProducts(List<Product> newProducts) {
        this.products = newProducts;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return products != null ? products.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout container;
        ImageView imageSneaker;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.sameSneakerContainer);
            imageSneaker = itemView.findViewById(R.id.imageSneaker);
        }
    }
}
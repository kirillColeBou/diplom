package com.example.sneaker_shop;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class ProductImagesAdapter extends RecyclerView.Adapter<ProductImagesAdapter.ImageViewHolder> {
    private List<String> imagesBase64;
    private LruCache<String, Bitmap> memoryCache;

    public ProductImagesAdapter(List<String> imagesBase64) {
        this.imagesBase64 = new ArrayList<>(imagesBase64);

        // Настройка кэша в памяти (примерно 1/8 доступной памяти)
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8;

        memoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    public void updateImages(List<String> newImages) {
        this.imagesBase64 = new ArrayList<>(newImages);
        memoryCache.evictAll(); // Очищаем кэш при обновлении изображений
        notifyDataSetChanged();
        Log.d("ProductImagesAdapter", "Images updated, new count: " + newImages.size());
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        if (position < 0 || position >= imagesBase64.size()) {
            Log.w("ProductImagesAdapter", "Invalid position: " + position);
            setDefaultImage(holder.imageView);
            return;
        }

        String imageData = imagesBase64.get(position);
        if (imageData == null || imageData.isEmpty()) {
            Log.w("ProductImagesAdapter", "Empty image at position: " + position);
            setDefaultImage(holder.imageView);
            return;
        }

        // Проверяем кэш
        Bitmap cachedBitmap = memoryCache.get(imageData);
        if (cachedBitmap != null && !cachedBitmap.isRecycled()) {
            Log.d("ProductImagesAdapter", "Using cached image for position: " + position);
            holder.imageView.setImageBitmap(cachedBitmap);
            return;
        }

        // Декодируем в фоновом потоке (в реальном приложении лучше использовать Glide/Picasso)
        new Thread(() -> {
            try {
                String base64Image = imageData.startsWith("data:image") ?
                        imageData.split(",")[1] : imageData;

                byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

                if (bitmap != null) {
                    // Кэшируем результат
                    memoryCache.put(imageData, bitmap);

                    holder.imageView.post(() -> {
                        if (bitmap.isRecycled()) {
                            setDefaultImage(holder.imageView);
                        } else {
                            holder.imageView.setImageBitmap(bitmap);
                        }
                    });
                } else {
                    throw new Exception("Failed to decode bitmap");
                }
            } catch (Exception e) {
                Log.e("ProductImagesAdapter", "Error decoding image: " + e.getMessage());
                holder.imageView.post(() -> setDefaultImage(holder.imageView));
            }
        }).start();
    }

    private void setDefaultImage(ImageView imageView) {
        imageView.setImageResource(R.drawable.nike_air_force);
    }

    @Override
    public int getItemCount() {
        return imagesBase64 != null ? imagesBase64.size() : 0;
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.productImage);
        }
    }
}
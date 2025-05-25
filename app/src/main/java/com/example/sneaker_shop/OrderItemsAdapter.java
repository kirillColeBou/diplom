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
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class OrderItemsAdapter extends RecyclerView.Adapter<OrderItemsAdapter.OrderItemViewHolder> {
    private final Context context;
    private List<CartItem> orderItems;

    public OrderItemsAdapter(List<CartItem> orderItems, Context context) {
        this.context = context;
        this.orderItems = orderItems != null ? new ArrayList<>(orderItems) : new ArrayList<>();
        Log.d("OrderItemsAdapter", "Initialized with " + this.orderItems.size() + " items");
    }

    @NonNull
    @Override
    public OrderItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.cart_item, parent, false);
        Log.d("OrderItemsAdapter", "ViewHolder created");
        return new OrderItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderItemViewHolder holder, int position) {
        CartItem item = orderItems.get(position);
        Log.d("OrderItemsAdapter", "Binding item at position " + position + ": product=" + (item.getProduct() != null ? item.getProduct().getName() : "null") + ", count=" + item.getCount() + ", size=" + item.getSize());
        if (item.getProduct() == null) {
            Log.e("OrderItemsAdapter", "Product is null for item at position " + position);
            return;
        }
        loadProductImage(item.getProduct().getId(), holder.productImage);
        holder.nameProduct.setText(item.getProduct().getName());
        holder.priceProduct.setText(String.format("%d ₽", (int) (item.getProduct().getPrice() * item.getCount())));
        holder.countText.setText(String.valueOf(item.getCount()));
        holder.sizeText.setText("Размер: " + item.getSize());
        holder.buttonsLayout.setVisibility(View.GONE);
    }

    private void loadProductImage(int productId, ImageView imageView) {
        String cacheKey = "product_" + productId + "_0";
        ImageCacheManager cacheManager = ImageCacheManager.getInstance(context);
        Bitmap cachedBitmap = cacheManager.getBitmapFromMemoryCache(cacheKey);
        if (cachedBitmap != null) {
            imageView.setImageBitmap(cachedBitmap);
            Log.d("OrderItemsAdapter", "Изображение загружено из кэша памяти: " + cacheKey);
            return;
        }
        cachedBitmap = cacheManager.getBitmapFromDiskCache(cacheKey);
        if (cachedBitmap != null) {
            cacheManager.addBitmapToMemoryCache(cacheKey, cachedBitmap);
            imageView.setImageBitmap(cachedBitmap);
            Log.d("OrderItemsAdapter", "Изображение загружено из кэша на диске: " + cacheKey);
            return;
        }
        ImageContext.loadImagesForProduct(context, productId, new ImageContext.ImagesCallback() {
            @Override
            public void onSuccess(List<String> images) {
                Log.d("OrderItemsAdapter", "Image loaded successfully, images count: " + (images != null ? images.size() : 0));
                if (images != null && !images.isEmpty() && !images.get(0).isEmpty()) {
                    try {
                        String base64Image = images.get(0).split(",")[1];
                        byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        imageView.setImageBitmap(decodedByte);
                        cacheManager.addBitmapToMemoryCache(cacheKey, decodedByte);
                        cacheManager.addBitmapToDiskCache(cacheKey, decodedByte);
                        Log.d("OrderItemsAdapter", "Изображение загружено из сети и закэшировано: " + cacheKey);
                    } catch (Exception e) {
                        Log.e("OrderItemsAdapter", "Ошибка декодирования изображения: " + e.getMessage());
                        imageView.setImageResource(R.drawable.nike_air_force);
                    }
                } else {
                    imageView.setImageResource(R.drawable.nike_air_force);
                }
            }

            @Override
            public void onError(String error) {
                Log.e("OrderItemsAdapter", "Ошибка загрузки изображений: " + error);
                imageView.setImageResource(R.drawable.nike_air_force);
            }
        });
    }

    @Override
    public int getItemCount() {
        int count = orderItems.size();
        Log.d("OrderItemsAdapter", "getItemCount: " + count);
        return count;
    }

    public void updateItems(List<CartItem> newItems) {
        Log.d("OrderItemsAdapter", "Updating items: newItems=" + (newItems != null ? newItems.size() : "null"));
        if (newItems == null) {
            orderItems = new ArrayList<>();
        } else {
            orderItems = new ArrayList<>(newItems);
        }
        Log.d("OrderItemsAdapter", "Items updated, new size: " + orderItems.size());
        notifyDataSetChanged();
    }

    static class OrderItemViewHolder extends RecyclerView.ViewHolder {
        TextView nameProduct, priceProduct, countText, sizeText;
        ImageView productImage;
        View buttonsLayout;

        OrderItemViewHolder(@NonNull View itemView) {
            super(itemView);
            nameProduct = itemView.findViewById(R.id.product_name);
            priceProduct = itemView.findViewById(R.id.product_price);
            countText = itemView.findViewById(R.id.count_text);
            sizeText = itemView.findViewById(R.id.product_size);
            productImage = itemView.findViewById(R.id.product_image);
            buttonsLayout = itemView.findViewById(R.id.buttons_layout);
            Log.d("OrderItemsAdapter", "ViewHolder initialized");
        }
    }
}
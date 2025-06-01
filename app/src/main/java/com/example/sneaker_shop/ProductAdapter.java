package com.example.sneaker_shop;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {
    private List<Product> productList;
    private Context context;
    private long currentUserId;
    private OnFavoriteClickListener favoriteClickListener;

    public interface OnFavoriteClickListener {
        void onFavoriteClick(int position, boolean isFavorite);
    }

    public ProductAdapter(Context context, List<Product> productList, long currentUserId, OnFavoriteClickListener listener) {
        this.context = context;
        this.productList = productList;
        this.currentUserId = currentUserId;
        this.favoriteClickListener = listener;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.product_item, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.nameProduct.setText(product.getName());
        holder.priceProduct.setText(String.format(Locale.getDefault(), "%.2f ₽", product.getPrice()));
        String cacheKey = "product_" + product.getId() + "_0";
        ImageCacheManager cacheManager = ImageCacheManager.getInstance(context);
        Bitmap cachedBitmap = cacheManager.getBitmapFromMemoryCache(cacheKey);

        // Установка размеров ImageView в зависимости от размера экрана
        setImageSize(holder.imageProduct);

        if (cachedBitmap != null) {
            holder.imageProduct.setImageBitmap(cachedBitmap);
        } else {
            cachedBitmap = cacheManager.getBitmapFromDiskCache(cacheKey);
            if (cachedBitmap != null) {
                cacheManager.addBitmapToMemoryCache(cacheKey, cachedBitmap);
                holder.imageProduct.setImageBitmap(cachedBitmap);
            } else {
                ImageContext.loadImagesForProduct(context, product.getId(), new ImageContext.ImagesCallback() {
                    @Override
                    public void onSuccess(List<String> images) {
                        if (images != null && !images.isEmpty() && !images.get(0).isEmpty()) {
                            try {
                                String base64Image = images.get(0).split(",")[1];
                                byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                holder.imageProduct.setImageBitmap(decodedByte);
                                cacheManager.addBitmapToMemoryCache(cacheKey, decodedByte);
                                cacheManager.addBitmapToDiskCache(cacheKey, decodedByte);
                                Log.d("ProductAdapter", "Image loaded from network and cached: " + cacheKey);
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
            }
        }

        FavoriteContext.checkFavorite(currentUserId, String.valueOf(product.getId()), new FavoriteContext.FavoriteCallback() {
            @Override
            public void onSuccess(boolean isFavorite) {
                int currentPosition = holder.getAdapterPosition();
                if (currentPosition != RecyclerView.NO_POSITION) {
                    holder.favoriteIcon.setImageResource(isFavorite ?
                            R.drawable.favorite_item_select : R.drawable.favorite_item);
                    holder.favoriteIcon.setTag(isFavorite);
                }
            }

            @Override
            public void onError(String error) {
                Log.e("ProductAdapter", "Error checking favorite status: " + error);
            }
        });

        holder.favoriteIcon.setOnClickListener(v -> {
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition == RecyclerView.NO_POSITION) return;
            Boolean isFavorite = (Boolean) holder.favoriteIcon.getTag();
            if (isFavorite == null) return;
            boolean newFavoriteState = !isFavorite;
            holder.favoriteIcon.setImageResource(newFavoriteState ?
                    R.drawable.favorite_item_select : R.drawable.favorite_item);
            holder.favoriteIcon.setTag(newFavoriteState);
            Product currentProduct = productList.get(currentPosition);
            FavoriteContext.toggleFavorite(currentUserId, String.valueOf(currentProduct.getId()), isFavorite,
                    new FavoriteContext.FavoriteCallback() {
                        @Override
                        public void onSuccess(boolean confirmedNewState) {
                            int updatedPosition = holder.getAdapterPosition();
                            if (updatedPosition != RecyclerView.NO_POSITION) {
                                holder.favoriteIcon.setImageResource(confirmedNewState ?
                                        R.drawable.favorite_item_select : R.drawable.favorite_item);
                                holder.favoriteIcon.setTag(confirmedNewState);
                                if (favoriteClickListener != null) {
                                    favoriteClickListener.onFavoriteClick(updatedPosition, confirmedNewState);
                                }
                            }
                        }

                        @Override
                        public void onError(String error) {
                            int updatedPosition = holder.getAdapterPosition();
                            if (updatedPosition != RecyclerView.NO_POSITION) {
                                holder.favoriteIcon.setImageResource(isFavorite ?
                                        R.drawable.favorite_item_select : R.drawable.favorite_item);
                                holder.favoriteIcon.setTag(isFavorite);
                                Log.e("ProductAdapter", "Error toggling favorite: " + error);
                            }
                        }
                    });
        });

        holder.itemView.setOnClickListener(v -> {
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION) {
                Product clickedProduct = productList.get(currentPosition);
                Log.d("ProductAdapter", "Item clicked: " + clickedProduct.getName() + " at position " + currentPosition);
                Intent intent = new Intent(context, ProductInfoActivity.class);
                intent.putExtra("product", clickedProduct);
                context.startActivity(intent);
            } else {
                Log.w("ProductAdapter", "Invalid position on item click: " + currentPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return productList != null ? productList.size() : 0;
    }

    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView imageProduct, favoriteIcon;
        TextView nameProduct, priceProduct;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            imageProduct = itemView.findViewById(R.id.image_product);
            nameProduct = itemView.findViewById(R.id.name_product);
            priceProduct = itemView.findViewById(R.id.price_product);
            favoriteIcon = itemView.findViewById(R.id.favorite_icon);
        }
    }

    private void setImageSize(ImageView imageView) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int smallestWidthDp = (int) (Math.min(displayMetrics.widthPixels, displayMetrics.heightPixels) / displayMetrics.density);
        int width, height;
        if (smallestWidthDp < 600) {
            width = 130;
            height = 70;
        } else if (smallestWidthDp >= 600 && smallestWidthDp <= 720) {
            width = 160;
            height = 80;
        } else {
            width = 320;
            height = 160;
        }
        int widthPx = (int) (width * displayMetrics.density);
        int heightPx = (int) (height * displayMetrics.density);
        ViewGroup.LayoutParams params = imageView.getLayoutParams();
        params.width = widthPx;
        params.height = heightPx;
        imageView.setLayoutParams(params);
    }
}
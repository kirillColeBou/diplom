package com.example.sneaker_shop;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
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

    public ProductImagesAdapter(List<String> imagesBase64) {
        this.imagesBase64 = new ArrayList<>(imagesBase64);
    }

    public void updateImages(List<String> newImages) {
        this.imagesBase64 = new ArrayList<>(newImages);
        notifyDataSetChanged();
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
        Log.d("ProductImagesAdapter", "Binding position: " + position + ", size: " + imagesBase64.size());
        if (position < 0 || position >= imagesBase64.size()) {
            Log.w("ProductImagesAdapter", "Invalid position: " + position + ", size: " + imagesBase64.size());
            holder.imageView.setImageResource(R.drawable.nike_air_force);
            return;
        }
        String imageData = imagesBase64.get(position);
        if (imageData.isEmpty()) {
            holder.imageView.setImageResource(R.drawable.nike_air_force);
            return;
        }
        try {
            String base64Image;
            if (imageData.startsWith("data:image")) {
                String[] parts = imageData.split(",");
                if (parts.length > 1) {
                    base64Image = parts[1];
                } else {
                    throw new IllegalArgumentException("Invalid data URI format");
                }
            } else {
                base64Image = imageData;
            }

            byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            if (decodedByte != null) {
                holder.imageView.setImageBitmap(decodedByte);
            } else {
                throw new IllegalArgumentException("Failed to decode bitmap");
            }
        } catch (Exception e) {
            Log.e("ProductImagesAdapter", "Error decoding image at position " + position + ": " + e.getMessage());
            holder.imageView.setImageResource(R.drawable.nike_air_force);
        }
    }

    @Override
    public int getItemCount() {
        return imagesBase64.size();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.productImage);
        }
    }
}
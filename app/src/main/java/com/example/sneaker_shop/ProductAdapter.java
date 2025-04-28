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

import java.util.List;
import java.util.Locale;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {
    private List<Product> productList;
    private Context context;

    public ProductAdapter(Context context, List<Product> productList) {
        this.context = context;
        this.productList = productList;
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

        if (product.getImage() != null && !product.getImage().isEmpty()) {
            try {
                String base64Image = product.getImage().split(",")[1];
                byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                holder.imageProduct.setImageBitmap(decodedByte);
            } catch (Exception e) {
                Log.e("ProductAdapter", "Error loading image", e);
                holder.imageProduct.setImageResource(R.drawable.nike_air_force);
            }
        }
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView imageProduct;
        TextView nameProduct;
        TextView priceProduct;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            imageProduct = itemView.findViewById(R.id.iamge_product);
            nameProduct = itemView.findViewById(R.id.name_product);
            priceProduct = itemView.findViewById(R.id.price_product);
        }
    }
}

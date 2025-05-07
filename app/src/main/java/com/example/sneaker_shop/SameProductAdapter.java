package com.example.sneaker_shop;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
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
        Product product = products.get(position);
        holder.container.setBackgroundResource(
                position == selectedPosition ?
                        R.drawable.background_same_sneaker_select :
                        R.drawable.background_same_sneaker
        );

        if (product.getImage() != null && !product.getImage().isEmpty()) {
            try {
                String base64Image = product.getImage().split(",")[1];
                byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                holder.imageSneaker.setImageBitmap(decodedByte);
            } catch (Exception e) {
                holder.imageSneaker.setImageResource(R.drawable.nike_air_force);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null) {
                itemClickListener.onItemClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return products.size();
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

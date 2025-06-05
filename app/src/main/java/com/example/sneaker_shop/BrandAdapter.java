package com.example.sneaker_shop;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BrandAdapter extends RecyclerView.Adapter<BrandAdapter.BrandViewHolder> {
    private final Context context;
    private List<Brands> brands;
    private Map<Integer, Boolean> selectedIds = new HashMap<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public BrandAdapter(List<Brands> brands, Context context, OnItemClickListener listener) {
        this.context = context;
        this.brands = brands;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BrandViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.filter_item, parent, false);
        return new BrandViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BrandViewHolder holder, int position) {
        Brands brand = brands.get(position);
        holder.itemName.setText(brand.getName());
        boolean isSelected = selectedIds.getOrDefault(brand.getId(), false);
        holder.itemView.setBackgroundResource(isSelected ? R.drawable.background_category_item_select : R.drawable.background_category_item);
        holder.itemName.setTextColor(isSelected ? context.getResources().getColor(android.R.color.white) : Color.parseColor("#2B2B2B"));
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();
        if (position == brands.size() - 1) {
            params.rightMargin = 0;
        } else {
            params.rightMargin = (int) context.getResources().getDimension(R.dimen.category_item_margin);
        }
        holder.itemView.setLayoutParams(params);
        holder.bind(isSelected);
    }

    @Override
    public int getItemCount() {
        return brands != null ? brands.size() : 0;
    }

    public class BrandViewHolder extends RecyclerView.ViewHolder {
        private TextView itemName;

        public BrandViewHolder(@NonNull View itemView) {
            super(itemView);
            itemName = itemView.findViewById(R.id.item_name);
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    Brands brand = brands.get(position);
                    boolean isSelected = selectedIds.getOrDefault(brand.getId(), false);
                    selectedIds.put(brand.getId(), !isSelected);
                    notifyItemChanged(position);
                    listener.onItemClick(position);
                }
            });
        }

        public void bind(boolean isSelected) {
        }
    }

    public List<Brands> getSelectedBrands() {
        List<Brands> selectedBrands = new ArrayList<>();
        for (Brands brand : brands) {
            if (selectedIds.getOrDefault(brand.getId(), false)) {
                selectedBrands.add(brand);
            }
        }
        return selectedBrands;
    }

    public void setSelectedIds(int[] ids) {
        selectedIds.clear();
        if (ids != null) {
            for (int id : ids) {
                selectedIds.put(id, true);
            }
        }
        notifyDataSetChanged();
    }

    public void clearSelections() {
        selectedIds.clear();
        notifyDataSetChanged();
    }
}
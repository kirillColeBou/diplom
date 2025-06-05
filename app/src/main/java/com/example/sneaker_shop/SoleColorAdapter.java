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

public class SoleColorAdapter extends RecyclerView.Adapter<SoleColorAdapter.ColorViewHolder> {
    private final Context context;
    private List<Colors> colors;
    private Map<Integer, Boolean> selectedIds = new HashMap<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public SoleColorAdapter(List<Colors> colors, Context context, OnItemClickListener listener) {
        this.context = context;
        this.colors = colors;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ColorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.filter_item, parent, false);
        return new ColorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ColorViewHolder holder, int position) {
        Colors color = colors.get(position);
        holder.itemName.setText(color.getName());
        boolean isSelected = selectedIds.getOrDefault(color.getId(), false);
        holder.itemView.setBackgroundResource(isSelected ? R.drawable.background_category_item_select : R.drawable.background_category_item);
        holder.itemName.setTextColor(isSelected ? context.getResources().getColor(android.R.color.white) : Color.parseColor("#2B2B2B"));
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();
        if (position == colors.size() - 1) {
            params.rightMargin = 0;
        } else {
            params.rightMargin = (int) context.getResources().getDimension(R.dimen.category_item_margin);
        }
        holder.itemView.setLayoutParams(params);
        holder.bind(isSelected);
    }

    @Override
    public int getItemCount() {
        return colors != null ? colors.size() : 0;
    }

    public class ColorViewHolder extends RecyclerView.ViewHolder {
        private TextView itemName;

        public ColorViewHolder(@NonNull View itemView) {
            super(itemView);
            itemName = itemView.findViewById(R.id.item_name);
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    Colors color = colors.get(position);
                    boolean isSelected = selectedIds.getOrDefault(color.getId(), false);
                    selectedIds.put(color.getId(), !isSelected);
                    notifyItemChanged(position);
                    listener.onItemClick(position);
                }
            });
        }

        public void bind(boolean isSelected) {
        }
    }

    public List<Colors> getSelectedColors() {
        List<Colors> selectedColors = new ArrayList<>();
        for (Colors color : colors) {
            if (selectedIds.getOrDefault(color.getId(), false)) {
                selectedColors.add(color);
            }
        }
        return selectedColors;
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
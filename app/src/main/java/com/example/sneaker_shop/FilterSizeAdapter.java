package com.example.sneaker_shop;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FilterSizeAdapter extends RecyclerView.Adapter<FilterSizeAdapter.SizeViewHolder> {
    private List<Size> sizes;
    private Map<Integer, Boolean> selectedIds = new HashMap<>();
    private OnSizeClickListener listener;

    public interface OnSizeClickListener {
        void onSizeClick(int position);
    }

    public FilterSizeAdapter(List<Size> sizes, OnSizeClickListener listener) {
        this.sizes = sizes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SizeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.size_item, parent, false);
        return new SizeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SizeViewHolder holder, int position) {
        Size size = sizes.get(position);
        holder.sizeText.setText(size.getValue());
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();
        if (position == sizes.size() - 1) {
            params.rightMargin = 0;
        } else {
            params.rightMargin = (int) holder.itemView.getContext().getResources().getDimension(R.dimen.category_item_margin);
        }
        holder.itemView.setLayoutParams(params);
        holder.bind(selectedIds.getOrDefault(size.getId(), false));
    }

    @Override
    public int getItemCount() {
        return sizes != null ? sizes.size() : 0;
    }

    public class SizeViewHolder extends RecyclerView.ViewHolder {
        private TextView sizeText;
        private View strikeThrough;
        private View dimOverlay;

        public SizeViewHolder(@NonNull View itemView) {
            super(itemView);
            sizeText = itemView.findViewById(R.id.sizeText);
            strikeThrough = itemView.findViewById(R.id.strikeThrough);
            dimOverlay = itemView.findViewById(R.id.dimOverlay);
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    Size size = sizes.get(position);
                    boolean isSelected = selectedIds.getOrDefault(size.getId(), false);
                    selectedIds.put(size.getId(), !isSelected);
                    notifyItemChanged(position);
                    listener.onSizeClick(position);
                }
            });
        }

        public void bind(boolean isSelected) {
            itemView.setBackgroundResource(isSelected ? R.drawable.background_size_item_select : R.drawable.background_size_item);
            if (strikeThrough != null && dimOverlay != null) {
                strikeThrough.setVisibility(View.GONE);
                dimOverlay.setVisibility(View.GONE);
            }
            itemView.setAlpha(1f);
            sizeText.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.black));
            sizeText.setTypeface(null, Typeface.NORMAL);
        }
    }

    public List<Size> getSelectedSizes() {
        List<Size> selectedSizes = new ArrayList<>();
        for (Size size : sizes) {
            if (selectedIds.getOrDefault(size.getId(), false)) {
                selectedSizes.add(size);
            }
        }
        return selectedSizes;
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
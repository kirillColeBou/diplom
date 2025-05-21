package com.example.sneaker_shop;

import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class SizeAdapter extends RecyclerView.Adapter<SizeAdapter.SizeViewHolder> {
    private List<SizeDisplayModel> displaySizes;
    private boolean isStoreSelected;
    private OnSizeSelectedListener listener;
    public int selectedPosition = -1;

    public interface OnSizeSelectedListener {
        void onSizeSelected(SizeDisplayModel size, boolean isAvailable);
    }

    public SizeAdapter(List<SizeDisplayModel> displaySizes, boolean isStoreSelected,
                       OnSizeSelectedListener listener) {
        this.displaySizes = displaySizes;
        this.isStoreSelected = isStoreSelected;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SizeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.size_item, parent, false);
        return new SizeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SizeViewHolder holder, int position) {
        SizeDisplayModel sizeModel = displaySizes.get(position);
        Log.d("SizeAdapter", "Binding size: " + sizeModel.getValue() +
                ", available: " + sizeModel.isAvailable() +
                ", count: " + sizeModel.getCount());
        holder.sizeText.setText(sizeModel.getValue());
        boolean isAvailable = isStoreSelected ? sizeModel.isAvailable() : true;
        holder.bind(isAvailable, position == selectedPosition, sizeModel.getCount());
        holder.itemView.setOnClickListener(v -> {
            if (!isAvailable) {
                Log.d("SizeAdapter", "Size not available: " + sizeModel.getValue());
                return;
            }
            int previousSelected = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            if (previousSelected != -1) {
                notifyItemChanged(previousSelected);
            }
            notifyItemChanged(selectedPosition);
            if (listener != null) {
                listener.onSizeSelected(sizeModel, isAvailable);
            }
            Log.d("SizeAdapter", "Size selected: " + sizeModel.getValue());
        });
    }

    @Override
    public int getItemCount() {
        return displaySizes.size();
    }

    public void updateSizes(List<SizeDisplayModel> newSizes) {
        this.displaySizes = newSizes;
        selectedPosition = -1;
        notifyDataSetChanged();
    }

    public SizeDisplayModel getSelectedSize() {
        if (selectedPosition >= 0 && selectedPosition < displaySizes.size()) {
            return displaySizes.get(selectedPosition);
        }
        return null;
    }

    static class SizeViewHolder extends RecyclerView.ViewHolder {
        TextView sizeText;
        View strikeThrough;
        View dimOverlay;

        SizeViewHolder(View itemView) {
            super(itemView);
            sizeText = itemView.findViewById(R.id.sizeText);
            strikeThrough = itemView.findViewById(R.id.strikeThrough);
            dimOverlay = itemView.findViewById(R.id.dimOverlay);
        }

        void bind(boolean isAvailable, boolean isSelected, int stockCount) {
            int backgroundRes = isSelected ?
                    R.drawable.background_size_item_select :
                    R.drawable.background_size_item;
            itemView.setBackgroundResource(backgroundRes);
            if (isAvailable) {
                strikeThrough.setVisibility(View.GONE);
                dimOverlay.setVisibility(View.GONE);
                itemView.setAlpha(1f);
                if (stockCount > 0 && stockCount < 5) {
                    sizeText.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_secondary));
                    sizeText.setTypeface(null, Typeface.BOLD);
                } else {
                    sizeText.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.black));
                    sizeText.setTypeface(null, Typeface.NORMAL);
                }
            } else {
                strikeThrough.setVisibility(View.VISIBLE);
                dimOverlay.setVisibility(View.VISIBLE);
                itemView.setAlpha(0.7f);
                sizeText.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.black));
                sizeText.setTypeface(null, Typeface.NORMAL);
            }
        }
    }
}
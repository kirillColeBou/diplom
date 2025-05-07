package com.example.sneaker_shop;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class SizeAdapter extends RecyclerView.Adapter<SizeAdapter.SizeViewHolder> {
    private List<Size> allSizes;
    public List<ProductSize> availableProductSizes;
    private OnSizeSelectedListener listener;
    public int selectedPosition = -1;

    public interface OnSizeSelectedListener {
        void onSizeSelected(Size size, boolean isAvailable);
    }

    public SizeAdapter(List<Size> allSizes, List<ProductSize> availableProductSizes,
                       OnSizeSelectedListener listener) {
        this.allSizes = allSizes;
        this.availableProductSizes = availableProductSizes;
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
        Size size = allSizes.get(position);
        boolean isAvailable = isSizeAvailable(size.getId());
        int stockCount = getStockCountForSize(size.getId());
        holder.sizeText.setText(size.getValue());
        holder.bind(isAvailable, position == selectedPosition, stockCount);
        holder.itemView.setOnClickListener(v -> {
            if (!isAvailable) return;
            int previousSelected = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            if (previousSelected != -1) {
                notifyItemChanged(previousSelected);
            }
            notifyItemChanged(selectedPosition);
            if (listener != null) {
                listener.onSizeSelected(size, true);
            }
        });
    }

    @Override
    public int getItemCount() {
        return allSizes.size();
    }

    private boolean isSizeAvailable(int sizeId) {
        for (ProductSize ps : availableProductSizes) {
            if (ps.getSizeId() == sizeId && ps.getCount() > 0) {
                return true;
            }
        }
        return false;
    }

    private int getStockCountForSize(int sizeId) {
        for (ProductSize ps : availableProductSizes) {
            if (ps.getSizeId() == sizeId) {
                return ps.getCount();
            }
        }
        return 0;
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
            } else {
                strikeThrough.setVisibility(View.VISIBLE);
                dimOverlay.setVisibility(View.VISIBLE);
                itemView.setAlpha(0.7f);
            }
            if (stockCount > 0 && stockCount < 5) {
                sizeText.setText(sizeText.getText());
            }
        }
    }
}

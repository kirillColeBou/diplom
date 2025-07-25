package com.example.sneaker_shop;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class FilterCategoryAdapter extends RecyclerView.Adapter<FilterCategoryAdapter.CategoryViewHolder> {
    private List<Category> categories;
    private OnCategoryClickListener listener;

    public interface OnCategoryClickListener {
        void onCategoryClick(int position);
    }

    public FilterCategoryAdapter(List<Category> categories, OnCategoryClickListener listener) {
        this.categories = categories;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.category_item, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categories.get(position);
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();
        if (position == categories.size() - 1) {
            params.rightMargin = 0;
        } else {
            params.rightMargin = (int) holder.itemView.getContext().getResources().getDimension(R.dimen.category_item_margin);
        }
        holder.itemView.setLayoutParams(params);
        holder.bind(category);
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    class CategoryViewHolder extends RecyclerView.ViewHolder {
        private TextView textView;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.text_category);
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Category category = categories.get(position);
                    category.setSelected(!category.isSelected());
                    notifyItemChanged(position);
                    listener.onCategoryClick(position);
                }
            });
        }

        public void bind(Category category) {
            textView.setText(category.getName());
            if (category.isSelected()) {
                itemView.setBackgroundResource(R.drawable.background_category_item_select);
                textView.setTextColor(Color.WHITE);
            } else {
                itemView.setBackgroundResource(R.drawable.background_category_item);
                textView.setTextColor(Color.parseColor("#2B2B2B"));
            }
        }
    }

    public List<Category> getSelectedCategories() {
        List<Category> selectedCategories = new ArrayList<>();
        for (Category category : categories) {
            if (category.isSelected()) {
                selectedCategories.add(category);
            }
        }
        return selectedCategories;
    }

    public void setSelectedIds(int[] ids) {
        for (Category category : categories) {
            category.setSelected(false);
        }
        if (ids != null) {
            for (int id : ids) {
                for (Category category : categories) {
                    if (category.getId() == id) {
                        category.setSelected(true);
                        break;
                    }
                }
            }
        }
        notifyDataSetChanged();
    }

    public void clearSelections() {
        for (Category category : categories) {
            category.setSelected(false);
        }
        notifyDataSetChanged();
    }
}
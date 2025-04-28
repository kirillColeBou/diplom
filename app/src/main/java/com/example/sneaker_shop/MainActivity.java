package com.example.sneaker_shop;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private RecyclerView categoriesRecyclerView;
    private CategoryAdapter categoryAdapter;
    private List<Category> categories = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        categoriesRecyclerView = findViewById(R.id.recycler_view_categories);
        categoriesRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        categoryAdapter = new CategoryAdapter(categories, position -> {
            for (int i = 0; i < categories.size(); i++) {
                categories.get(i).setSelected(i == position);
            }
            categoryAdapter.notifyDataSetChanged();
            loadProductsForCategory(categories.get(position).getId());
        });
        categoriesRecyclerView.setAdapter(categoryAdapter);
        loadCategories();
    }

    private void loadCategories() {
        CategoryContext.loadCategories(new CategoryContext.CategoriesCallback() {
            @Override
            public void onSuccess(List<Category> loadedCategories) {
                runOnUiThread(() -> {
                    categories.clear();
                    categories.addAll(loadedCategories);
                    categoryAdapter.notifyDataSetChanged();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Ошибка загрузки категорий", Toast.LENGTH_SHORT).show();
                    Log.e("MainActivity", error);
                });
            }
        });
    }

    private void loadProductsForCategory(int categoryId) {

    }
}

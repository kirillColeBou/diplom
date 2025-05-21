package com.example.sneaker_shop;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class SearchResultActivity extends AppCompatActivity
        implements ProductAdapter.OnFavoriteClickListener {
    private RecyclerView productsRecyclerView;
    private ProductAdapter productAdapter;
    private List<Product> productList = new ArrayList<>();
    private long currentUserId;
    private String searchQuery;
    private LinearLayout emptySearchLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_result_activity);
        currentUserId = AuthUtils.getCurrentUserId(this);
        searchQuery = getIntent().getStringExtra("search_query");
        TextView searchTextView = findViewById(R.id.searchResult);
        searchTextView.setText(searchQuery);
        emptySearchLayout = findViewById(R.id.empty_search_item);
        initProductsRecyclerView();
        loadSearchResults();
        checkEmptyState();
    }

    private void checkEmptyState() {
        if (productList.isEmpty()) {
            productsRecyclerView.setVisibility(View.GONE);
            emptySearchLayout.setVisibility(View.VISIBLE);
        } else {
            productsRecyclerView.setVisibility(View.VISIBLE);
            emptySearchLayout.setVisibility(View.GONE);
        }
    }

    private void initProductsRecyclerView() {
        productsRecyclerView = findViewById(R.id.findElement);
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        productsRecyclerView.setLayoutManager(layoutManager);
        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.item_spacing);
        productsRecyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                outRect.left = spacingInPixels;
                outRect.right = spacingInPixels;
                outRect.bottom = spacingInPixels;
                if (parent.getChildAdapterPosition(view) < 2) {
                    outRect.top = spacingInPixels;
                }
            }
        });
        productsRecyclerView.setClipToPadding(false);
        productsRecyclerView.setPadding(spacingInPixels, spacingInPixels, spacingInPixels, spacingInPixels);
        productAdapter = new ProductAdapter(this, productList, currentUserId, this);
        productsRecyclerView.setAdapter(productAdapter);
    }

    @Override
    public void onFavoriteClick(int position, boolean newFavoriteState) {
        if (position < 0 || position >= productList.size()) return;
        Product product = productList.get(position);
        FavoriteContext.toggleFavorite(currentUserId, String.valueOf(product.getId()), newFavoriteState,
                new FavoriteContext.FavoriteCallback() {
                    @Override
                    public void onSuccess(boolean serverConfirmedState) {
                        runOnUiThread(() -> {
                            productAdapter.notifyItemChanged(position);
                            if (serverConfirmedState == newFavoriteState) {
                                Toast.makeText(SearchResultActivity.this,
                                        newFavoriteState ? "Добавлено в избранное" : "Удалено из избранного",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(SearchResultActivity.this,
                                        "Состояние избранного не изменилось",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            productAdapter.notifyItemChanged(position);
                            Toast.makeText(SearchResultActivity.this,
                                    "Ошибка сети: " + error,
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    private void loadSearchResults() {
        ProductContext.loadProducts(new ProductContext.ProductsCallback() {
            @Override
            public void onSuccess(List<Product> products) {
                runOnUiThread(() -> {
                    productList.clear();
                    for (Product product : products) {
                        if (product.getName().toLowerCase().contains(searchQuery.toLowerCase())) {
                            productList.add(product);
                        }
                    }
                    productAdapter.notifyDataSetChanged();
                    checkEmptyState();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(SearchResultActivity.this,
                            "Ошибка загрузки товаров", Toast.LENGTH_SHORT).show();
                    checkEmptyState();
                });
            }
        });
    }

    public void onBack(View view) {
        startActivity(new Intent(this, SearchActivity.class));
        overridePendingTransition(0, 0);
        finish();
    }

    public void onDeleteSearchText(View view) {
        Intent intent = new Intent(this, SearchActivity.class);
        intent.putExtra("clear_text", true);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }

    public void onHome(View view) {
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(0, 0);
        finish();
    }

    public void onMenu(View view) {
        startActivity(new Intent(this, MenuActivity.class));
        overridePendingTransition(0, 0);
        finish();
    }

    public void onFavorite(View view) {
        startActivity(new Intent(this, FavoriteActivity.class));
        overridePendingTransition(0, 0);
        finish();
    }

    public void onPerson(View view) {
        startActivity(new Intent(this, PersonActivity.class));
        overridePendingTransition(0, 0);
        finish();
    }
}
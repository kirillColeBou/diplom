package com.example.sneaker_shop;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.yandex.mobile.ads.banner.BannerAdSize;
import com.yandex.mobile.ads.banner.BannerAdView;
import com.yandex.mobile.ads.common.AdRequest;
import com.yandex.mobile.ads.common.InitializationListener;
import com.yandex.mobile.ads.common.MobileAds;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ProductAdapter.OnFavoriteClickListener, CategoryAdapter.OnCategoryClickListener {
    private RecyclerView categoriesRecyclerView;
    private CategoryAdapter categoryAdapter;
    private List<Category> categories = new ArrayList<>();
    private RecyclerView productsRecyclerView;
    private ProductAdapter productAdapter;
    private List<Product> productList = new ArrayList<>();
    private SwipeRefreshLayout swipeRefresh;
    private BannerAdView adView;
    private long currentUserId;
    private Category currentSelectedCategory;
    private static final int REQUEST_SELECT_STORE = 1;
    private TextView storeSelectionText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!AuthUtils.isUserLoggedIn(this)) {
            startActivity(new Intent(this, AuthorizationActivity.class));
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            finish();
            return;
        }
        setContentView(R.layout.main_activity);
        currentUserId = AuthUtils.getCurrentUserId(this);
        Log.d("main", String.valueOf(currentUserId));
        MobileAds.initialize(this, new InitializationListener() {
            @Override
            public void onInitializationCompleted() {
                Log.d("MobileAds", "Initialization completed");
            }
        });
        storeSelectionText = findViewById(R.id.store_selection_text);
        storeSelectionText.setText(PreferencesHelper.getSelectedStoreAddress(this));
        adView = findViewById(R.id.adView);
        adView.setAdUnitId("demo-appopenad-yandex");
        adView.setAdSize(BannerAdSize.fixedSize(this, 400, 150));
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
        initCategories();
        swipeRefresh = findViewById(R.id.swipeRefresh);
        swipeRefresh.setOnRefreshListener(() -> {
            if (currentSelectedCategory != null) {
                if (currentSelectedCategory.getId() == -1) {
                    loadRecommendedProducts();
                } else if (currentSelectedCategory.getId() == -2) {
                    loadProducts();
                } else {
                    loadProductsForCategory(currentSelectedCategory.getId());
                }
            } else {
                loadProducts();
            }
        });
        swipeRefresh.setProgressViewOffset(false, 0, 100);
        swipeRefresh.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
        loadCategories();
        currentSelectedCategory = new Category(-2, "Все модели", true);
        initProducts();
        loadProducts();
    }

    private void initCategories() {
        categoriesRecyclerView = findViewById(R.id.recycler_view_categories);
        categoriesRecyclerView.setLayoutManager(new LinearLayoutManager(
                this, LinearLayoutManager.HORIZONTAL, false));
        categoryAdapter = new CategoryAdapter(categories, this);
        categoriesRecyclerView.setAdapter(categoryAdapter);
        loadCategories();
    }

    @Override
    public void onCategoryClick(int position) {
        for (Category category : categories) {
            category.setSelected(false);
        }
        categories.get(position).setSelected(true);
        currentSelectedCategory = categories.get(position);
        categoryAdapter.notifyDataSetChanged();
        if (currentSelectedCategory.getId() == -1) {
            loadRecommendedProducts();
        } else if (currentSelectedCategory.getId() == -2) {
            loadProducts();
        } else {
            loadProductsForCategory(currentSelectedCategory.getId());
        }
    }

    private void loadRecommendedProducts() {
        swipeRefresh.setRefreshing(true);
        long userUid = AuthUtils.getCurrentUserId(this);
        ProductContext.loadRecommendedProducts(userUid, new ProductContext.ProductsCallback() {
            @Override
            public void onSuccess(List<Product> products) {
                runOnUiThread(() -> {
                    productList.clear();
                    productList.addAll(products);
                    productAdapter.notifyDataSetChanged();
                    swipeRefresh.setRefreshing(false);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    loadProducts();
                    Toast.makeText(MainActivity.this,
                            "Рекомендации временно недоступны",
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void initProducts() {
        productsRecyclerView = findViewById(R.id.recycler_view_products);
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        productsRecyclerView.setLayoutManager(layoutManager);
        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.item_spacing);
        productsRecyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                outRect.left = spacingInPixels / 2;
                outRect.right = spacingInPixels / 2;
                outRect.bottom = spacingInPixels;
                if (parent.getChildAdapterPosition(view) < 2) {
                    outRect.top = spacingInPixels;
                }
            }
        });

        productsRecyclerView.setClipToPadding(false);
        productsRecyclerView.setPadding(spacingInPixels, 0, spacingInPixels, 0);
        productAdapter = new ProductAdapter(this, productList, currentUserId, this);
        productsRecyclerView.setAdapter(productAdapter);
    }

    private void loadProducts() {
        ProductContext.loadProducts(new ProductContext.ProductsCallback() {
            @Override
            public void onSuccess(List<Product> products) {
                runOnUiThread(() -> {
                    productList.clear();
                    productList.addAll(products);
                    productAdapter.notifyDataSetChanged();
                    swipeRefresh.setRefreshing(false);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Ошибка загрузки", Toast.LENGTH_SHORT).show();
                    swipeRefresh.setRefreshing(false);
                });
            }
        });
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
        swipeRefresh.setRefreshing(true);
        ProductContext.loadProductsByCategory(categoryId, new ProductContext.ProductsCallback() {
            @Override
            public void onSuccess(List<Product> products) {
                runOnUiThread(() -> {
                    productList.clear();
                    productList.addAll(products);
                    productAdapter.notifyDataSetChanged();
                    swipeRefresh.setRefreshing(false);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this,
                            "Ошибка загрузки товаров категории",
                            Toast.LENGTH_SHORT).show();
                    swipeRefresh.setRefreshing(false);
                });
            }
        });
    }

    @Override
    public void onFavoriteClick(int position, boolean isFavorite) {
        Product product = productList.get(position);
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

    public void onSearch(View view) {
        startActivity(new Intent(this, SearchActivity.class));
        overridePendingTransition(0, 0);
        finish();
    }

    public void onMapActivity(View view) {
        Intent intent = new Intent(this, MapActivity.class);
        startActivityForResult(intent, REQUEST_SELECT_STORE);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SELECT_STORE && resultCode == RESULT_OK && data != null) {
            String storeAddress = data.getStringExtra("store_address");
            storeSelectionText.setText(storeAddress);
            PreferencesHelper.saveSelectedStoreAddress(this, storeAddress);
        }
    }
}

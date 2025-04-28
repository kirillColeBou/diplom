package com.example.sneaker_shop;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
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

public class MainActivity extends AppCompatActivity {
    private RecyclerView categoriesRecyclerView;
    private CategoryAdapter categoryAdapter;
    private List<Category> categories = new ArrayList<>();
    private RecyclerView productsRecyclerView;
    private ProductAdapter productAdapter;
    private List<Product> productList = new ArrayList<>();
    private SwipeRefreshLayout swipeRefresh;
    private BannerAdView adView;

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
        MobileAds.initialize(this, new InitializationListener() {
            @Override
            public void onInitializationCompleted() {
                Log.d("MobileAds", "Initialization completed");
            }
        });
        adView = findViewById(R.id.adView);
        adView.setAdUnitId("demo-appopenad-yandex");
        adView.setAdSize(BannerAdSize.fixedSize(this, 400, 150));
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        swipeRefresh.setOnRefreshListener(() -> {
            loadProducts();
        });
        swipeRefresh.setProgressViewOffset(false, 0, 100);
        swipeRefresh.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
        initCategories();
        initProducts();
        loadProducts();
    }

    private void initCategories() {
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
        productAdapter = new ProductAdapter(this, productList);
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

    }

    public void onMenu(View view){

    }

    public void onFavorite(View view){

    }

    public void onCart(View view){

    }

    public void onPerson(View view){

    }

    public void onSearch(View view){

    }
}

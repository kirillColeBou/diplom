package com.example.sneaker_shop;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FilterResultActivity extends AppCompatActivity
        implements ProductAdapter.OnFavoriteClickListener {
    private RecyclerView filterResultRecyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout emptyFilterResult;
    private ProductAdapter adapter;
    private List<Product> products = new ArrayList<>();
    private long currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.filter_result_activity);
        currentUserId = AuthUtils.getCurrentUserId(this);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        emptyFilterResult = findViewById(R.id.empty_filter_result);
        initProductsRecyclerView();
        swipeRefresh.setOnRefreshListener(this::loadProducts);
        swipeRefresh.setColorSchemeResources(android.R.color.holo_blue_bright);
        loadProducts();
    }

    private void initProductsRecyclerView() {
        filterResultRecyclerView = findViewById(R.id.filterResultRecyclerView);
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        filterResultRecyclerView.setLayoutManager(layoutManager);
        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.item_spacing);
        filterResultRecyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
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
        filterResultRecyclerView.setClipToPadding(false);
        filterResultRecyclerView.setPadding(spacingInPixels, spacingInPixels, spacingInPixels, spacingInPixels);
        adapter = new ProductAdapter(this, products, currentUserId, this);
        filterResultRecyclerView.setAdapter(adapter);
    }

    private void loadProducts() {
        swipeRefresh.setRefreshing(true);
        Intent intent = getIntent();
        double minPrice = intent.getDoubleExtra("minPrice", 0.0);
        double maxPrice = intent.getDoubleExtra("maxPrice", 100000.0);
        int[] brandIds = intent.getIntArrayExtra("brandIds");
        List<Integer> brandIdList = brandIds != null ? Arrays.asList(Arrays.stream(brandIds).boxed().toArray(Integer[]::new)) : new ArrayList<>();
        int[] shoeColorIds = intent.getIntArrayExtra("shoeColorIds");
        List<Integer> shoeColorIdList = shoeColorIds != null ? Arrays.asList(Arrays.stream(shoeColorIds).boxed().toArray(Integer[]::new)) : new ArrayList<>();
        int[] soleColorIds = intent.getIntArrayExtra("soleColorIds");
        List<Integer> soleColorIdList = soleColorIds != null ? Arrays.asList(Arrays.stream(soleColorIds).boxed().toArray(Integer[]::new)) : new ArrayList<>();
        int[] categoryIds = intent.getIntArrayExtra("categoryIds");
        List<Integer> categoryIdList = categoryIds != null ? Arrays.asList(Arrays.stream(categoryIds).boxed().toArray(Integer[]::new)) : new ArrayList<>();
        int[] sizeIds = intent.getIntArrayExtra("sizeIds");
        List<Integer> sizeIdList = sizeIds != null ? Arrays.asList(Arrays.stream(sizeIds).boxed().toArray(Integer[]::new)) : new ArrayList<>();
        int storeId = intent.getIntExtra("storeId", -1); // -1 как значение по умолчанию
        if (storeId == -1) {
            Toast.makeText(this, "Магазин не выбран", Toast.LENGTH_SHORT).show();
            swipeRefresh.setRefreshing(false);
            return;
        }
        ProductContext.loadProducts(new ProductContext.ProductsCallback() {
            @SuppressLint("StaticFieldLeak")
            @Override
            public void onSuccess(List<Product> allProducts) {
                List<Product> preFilteredProducts = new ArrayList<>();
                for (Product product : allProducts) {
                    boolean matchesPrice = product.getPrice() >= minPrice && product.getPrice() <= maxPrice;
                    boolean matchesBrand = brandIdList.isEmpty() || brandIdList.contains(product.getBrandId());
                    boolean matchesShoeColor = shoeColorIdList.isEmpty() || shoeColorIdList.contains(product.getShoeColorId());
                    boolean matchesSoleColor = soleColorIdList.isEmpty() || soleColorIdList.contains(product.getSoleColorId());
                    boolean matchesCategory = categoryIdList.isEmpty() || categoryIdList.contains(product.getCategoryId());
                    if (matchesPrice && matchesBrand && matchesShoeColor && matchesSoleColor && matchesCategory) {
                        preFilteredProducts.add(product);
                    }
                }
                if (preFilteredProducts.isEmpty()) {
                    products.clear();
                    runOnUiThread(() -> {
                        adapter.notifyDataSetChanged();
                        checkEmptyState();
                        swipeRefresh.setRefreshing(false);
                    });
                    return;
                }
                if (sizeIdList.isEmpty()) {
                    products.clear();
                    products.addAll(preFilteredProducts);
                    runOnUiThread(() -> {
                        adapter.notifyDataSetChanged();
                        checkEmptyState();
                        swipeRefresh.setRefreshing(false);
                    });
                    return;
                }
                List<Integer> productIds = new ArrayList<>();
                for (Product product : preFilteredProducts) {
                    productIds.add(product.getId());
                }
                String productSizeUrl = "https://mgxymxiehfsptuubuqfv.supabase.co/rest/v1/product_size" +
                        "?product_id=in.(" + String.join(",", productIds.stream().map(String::valueOf).toArray(String[]::new)) + ")" +
                        "&store_id=eq." + storeId +
                        "&select=id,product_id,size_id,count,store_id";
                new AsyncTask<Void, Void, List<ProductSize>>() {
                    private String error;

                    @Override
                    protected List<ProductSize> doInBackground(Void... voids) {
                        List<ProductSize> productSizes = new ArrayList<>();
                        try {
                            Document doc = Jsoup.connect(productSizeUrl)
                                    .header("Authorization", UserContext.TOKEN())
                                    .header("apikey", UserContext.SECRET())
                                    .ignoreContentType(true)
                                    .get();
                            String response = doc.body().text();
                            JSONArray jsonArray = new JSONArray(response);
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject obj = jsonArray.getJSONObject(i);
                                ProductSize productSize = new ProductSize(
                                        obj.getInt("id"),
                                        obj.getInt("product_id"),
                                        obj.getInt("size_id"),
                                        obj.getInt("count"),
                                        obj.getInt("store_id")
                                );
                                productSizes.add(productSize);
                            }
                            return productSizes;
                        } catch (Exception e) {
                            error = "Error loading product sizes: " + e.getMessage();
                            return null;
                        }
                    }

                    @Override
                    protected void onPostExecute(List<ProductSize> productSizes) {
                        if (error != null) {
                            runOnUiThread(() -> {
                                Toast.makeText(FilterResultActivity.this, error, Toast.LENGTH_SHORT).show();
                                checkEmptyState();
                                swipeRefresh.setRefreshing(false);
                            });
                            return;
                        }
                        Set<Integer> availableProductIds = new HashSet<>();
                        for (ProductSize productSize : productSizes) {
                            if (productSize.getCount() > 0 && sizeIdList.contains(productSize.getSizeId())) {
                                availableProductIds.add(productSize.getProductId());
                            }
                        }
                        products.clear();
                        for (Product product : preFilteredProducts) {
                            if (availableProductIds.contains(product.getId())) {
                                products.add(product);
                            }
                        }
                        runOnUiThread(() -> {
                            adapter.notifyDataSetChanged();
                            checkEmptyState();
                            swipeRefresh.setRefreshing(false);
                        });
                    }
                }.execute();
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(FilterResultActivity.this, "Ошибка загрузки данных: " + error, Toast.LENGTH_SHORT).show();
                    checkEmptyState();
                    swipeRefresh.setRefreshing(false);
                });
            }
        });
    }

    private void checkEmptyState() {
        if (products.isEmpty()) {
            filterResultRecyclerView.setVisibility(View.GONE);
            emptyFilterResult.setVisibility(View.VISIBLE);
        } else {
            filterResultRecyclerView.setVisibility(View.VISIBLE);
            emptyFilterResult.setVisibility(View.GONE);
        }
    }

    @Override
    public void onFavoriteClick(int position, boolean newFavoriteState) {
        if (position < 0 || position >= products.size()) return;
        Product product = products.get(position);
    }

    public void onBack(View view) {
        Intent intent = new Intent(this, FilterActivity.class);
        intent.putExtra("minPrice", getIntent().getDoubleExtra("minPrice", 0.0));
        intent.putExtra("maxPrice", getIntent().getDoubleExtra("maxPrice", 100000.0));
        intent.putExtra("brandIds", getIntent().getIntArrayExtra("brandIds"));
        intent.putExtra("shoeColorIds", getIntent().getIntArrayExtra("shoeColorIds"));
        intent.putExtra("soleColorIds", getIntent().getIntArrayExtra("soleColorIds"));
        intent.putExtra("categoryIds", getIntent().getIntArrayExtra("categoryIds"));
        intent.putExtra("sizeIds", getIntent().getIntArrayExtra("sizeIds"));
        intent.putExtra("storeId", getIntent().getIntExtra("storeId", -1));
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        loadProducts();
    }
}
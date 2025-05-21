package com.example.sneaker_shop;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProductInfoActivity extends AppCompatActivity implements SizeAdapter.OnSizeSelectedListener {
    private TextView descriptionTextView;
    private TextView expandCollapseButton;
    private LinearLayout descriptionContainer;
    private RecyclerView sizesRecyclerView;
    private TextView stockCountTextView;
    private ImageView favoriteIcon;
    private LinearLayout favoriteButton;
    private RecyclerView sameProductsRecyclerView;
    private LinearLayout sameProductsContainer;
    private ViewPager2 productImagesPager;
    private LinearLayout imagesIndicator;
    private ProgressBar topProgressBar;
    private ScrollView contentScrollView;
    private SizeAdapter sizeAdapter;
    private SameProductAdapter sameProductAdapter;
    private ProductImagesAdapter imagesAdapter;
    private List<String> productImages = new ArrayList<>();
    private boolean isDescriptionExpanded = false;
    private boolean isFavorite = false;
    private long currentUserId;
    private Product currentProduct;
    private int collapsedHeight;
    private int expandedHeight;
    private boolean isInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.product_info_activity);
        initViews();
        setupProgressBar();
        currentUserId = AuthUtils.getCurrentUserId(this);
        currentProduct = (Product) getIntent().getSerializableExtra("product");
        if (currentProduct != null) {
            loadInitialData();
        }
    }

    private void initViews() {
        descriptionTextView = findViewById(R.id.productDescription);
        expandCollapseButton = findViewById(R.id.expandCollapseButton);
        descriptionContainer = findViewById(R.id.descriptionContainer);
        sizesRecyclerView = findViewById(R.id.sizesRecyclerView);
        stockCountTextView = findViewById(R.id.stockCountTextView);
        favoriteButton = findViewById(R.id.favoriteButton);
        favoriteIcon = findViewById(R.id.favoriteIcon);
        sameProductsRecyclerView = findViewById(R.id.sameProductsRecyclerView);
        sameProductsContainer = findViewById(R.id.sameProductsContainer);
        productImagesPager = findViewById(R.id.productImagesPager);
        imagesIndicator = findViewById(R.id.imagesIndicator);
        topProgressBar = findViewById(R.id.topProgressBar);
        contentScrollView = findViewById(R.id.contentScrollView);
        contentScrollView.setVisibility(View.INVISIBLE);
    }

    private void setupProgressBar() {
        topProgressBar.setVisibility(View.VISIBLE);
        topProgressBar.setIndeterminate(true);
        contentScrollView.setVisibility(View.INVISIBLE);
    }

    private void updateBasicProductInfo(Product product) {
        TextView productNameTextView = findViewById(R.id.productName);
        TextView priceTextView = findViewById(R.id.productPrice);
        productNameTextView.setText(product.getName());
        priceTextView.setText(String.format("₽ %.2f", product.getPrice()));
        descriptionTextView.setText(product.getDescription());
    }

    private void loadHeavyData(boolean isFullLoad, Runnable onComplete) {
        ExecutorService executor = Executors.newFixedThreadPool(3);
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(isFullLoad ? 4 : 3);
        java.util.concurrent.atomic.AtomicBoolean hasError = new java.util.concurrent.atomic.AtomicBoolean(false);
        executor.submit(() -> {
            try {
                loadProductImages(currentProduct.getId());
            } catch (Exception e) {
                hasError.set(true);
                Log.e("ProductInfoActivity", "Image loading error: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });
        executor.submit(() -> {
            try {
                loadSizesForProduct(currentProduct.getId());
            } catch (Exception e) {
                hasError.set(true);
                Log.e("ProductInfoActivity", "Sizes loading error: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });
        if (isFullLoad) {
            executor.submit(() -> {
                try {
                    loadSameProducts();
                    runOnUiThread(() -> {
                        setupListeners();
                        setupDescriptionAnimation();
                    });
                } catch (Exception e) {
                    hasError.set(true);
                    Log.e("ProductInfoActivity", "Same products loading error: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        } else {
            runOnUiThread(this::setupDescriptionAnimation);
            latch.countDown();
        }
        executor.submit(() -> {
            try {
                checkFavoriteStatus();
            } catch (Exception e) {
                hasError.set(true);
                Log.e("ProductInfoActivity", "Favorite status error: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });
        executor.submit(() -> {
            try {
                latch.await();
                if (hasError.get()) {
                    runOnUiThread(() -> {
                        Toast.makeText(ProductInfoActivity.this,
                                "Ошибка загрузки данных", Toast.LENGTH_LONG).show();
                        startActivity(new Intent(ProductInfoActivity.this, MainActivity.class));
                        finish();
                    });
                } else {
                    onComplete.run();
                }
            } catch (InterruptedException e) {
                Log.e("ProductInfoActivity", "Error waiting for tasks: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(ProductInfoActivity.this,
                            "Ошибка загрузки данных", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(ProductInfoActivity.this, MainActivity.class));
                    finish();
                });
            }
        });
        executor.shutdown();
    }

    private void loadProductImages(int productId) {
        ImageContext.loadImagesForProduct(productId, new ImageContext.ImagesCallback() {
            @Override
            public void onSuccess(List<String> images) {
                runOnUiThread(() -> {
                    productImages.clear();
                    productImages.addAll(images.isEmpty() ? Collections.singletonList("") : images);
                    setupImagesPager();
                });
            }

            @Override
            public void onError(String error) {
                Log.e("ProductInfo", "Error loading images: " + error);
                runOnUiThread(() -> {
                    productImages.clear();
                    productImages.add("");
                    setupImagesPager();
                });
            }
        });
    }

    private void setupImagesPager() {
        if (productImages.isEmpty() || productImages.contains("")) {
            productImages.clear();
            productImages.add("");
        }
        if (imagesAdapter == null) {
            imagesAdapter = new ProductImagesAdapter(productImages);
            productImagesPager.setAdapter(imagesAdapter);
            productImagesPager.setOffscreenPageLimit(1);
            productImagesPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);
                    updateImagesIndicator(position);
                }
            });
        } else {
            imagesAdapter.updateImages(productImages);
        }
        setupImagesIndicator();
    }

    private void setupImagesIndicator() {
        runOnUiThread(() -> {
            imagesIndicator.removeAllViews();
            int selectedSize = getResources().getDimensionPixelSize(R.dimen.dot_selected_size);
            int unselectedSize = getResources().getDimensionPixelSize(R.dimen.dot_unselected_size);
            int margin = getResources().getDimensionPixelSize(R.dimen.dot_margin);
            for (int i = 0; i < productImages.size(); i++) {
                View dot = new View(this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        unselectedSize,
                        unselectedSize
                );
                params.setMargins(margin, 0, margin, 0);
                params.gravity = Gravity.CENTER_VERTICAL;
                dot.setLayoutParams(params);
                dot.setBackground(getResources().getDrawable(R.drawable.dot_unselected));
                imagesIndicator.addView(dot);
            }
            if (!productImages.isEmpty()) {
                updateImagesIndicator(0);
            }
        });
    }

    private void updateImagesIndicator(int position) {
        runOnUiThread(() -> {
            int selectedSize = getResources().getDimensionPixelSize(R.dimen.dot_selected_size);
            int unselectedSize = getResources().getDimensionPixelSize(R.dimen.dot_unselected_size);
            for (int i = 0; i < imagesIndicator.getChildCount(); i++) {
                View dot = imagesIndicator.getChildAt(i);
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) dot.getLayoutParams();
                if (i == position) {
                    params.width = selectedSize;
                    params.height = selectedSize;
                    dot.setBackground(getResources().getDrawable(R.drawable.dot_selected));
                } else {
                    params.width = unselectedSize;
                    params.height = unselectedSize;
                    dot.setBackground(getResources().getDrawable(R.drawable.dot_unselected));
                }
                dot.setLayoutParams(params);
            }
        });
    }

    private void setupListeners() {
        favoriteButton.setOnClickListener(v -> toggleFavorite());
        expandCollapseButton.setOnClickListener(v -> toggleDescription());
    }

    private void setupDescriptionAnimation() {
        resetDescriptionState();
        descriptionContainer.post(() -> {
            descriptionTextView.setMaxLines(3);
            descriptionTextView.setEllipsize(TextUtils.TruncateAt.END);
            descriptionContainer.measure(
                    View.MeasureSpec.makeMeasureSpec(descriptionContainer.getWidth(), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            collapsedHeight = descriptionContainer.getMeasuredHeight();
            Log.d("ProductInfoActivity", "Collapsed height: " + collapsedHeight);
            descriptionTextView.setMaxLines(Integer.MAX_VALUE);
            descriptionTextView.setEllipsize(null);
            descriptionContainer.measure(
                    View.MeasureSpec.makeMeasureSpec(descriptionContainer.getWidth(), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            expandedHeight = descriptionContainer.getMeasuredHeight();
            Log.d("ProductInfoActivity", "Expanded height: " + expandedHeight);
            descriptionTextView.setMaxLines(3);
            descriptionTextView.setEllipsize(TextUtils.TruncateAt.END);
            descriptionContainer.requestLayout();
            if (collapsedHeight >= expandedHeight) {
                expandCollapseButton.setVisibility(View.GONE);
                isInitialized = false;
                Log.d("ProductInfoActivity", "Description too short, hiding button");
            } else {
                expandCollapseButton.setVisibility(View.VISIBLE);
                isInitialized = true;
                Log.d("ProductInfoActivity", "Description expandable, button visible");
            }
        });
    }

    private void loadSameProducts() {
        if (currentProduct == null) return;
        ProductContext.loadProducts(new ProductContext.ProductsCallback() {
            @Override
            public void onSuccess(List<Product> allProducts) {
                List<Product> sameProducts = findSameProducts(allProducts);
                runOnUiThread(() -> {
                    if (sameProducts.size() > 1) {
                        setupSameProductsRecyclerView(sameProducts);
                        sameProductsContainer.setVisibility(View.VISIBLE);
                    } else {
                        sameProductsContainer.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e("ProductInfo", "Error loading same products: " + error);
                runOnUiThread(() -> sameProductsContainer.setVisibility(View.GONE));
            }
        });
    }

    private List<Product> findSameProducts(List<Product> allProducts) {
        List<Product> result = new ArrayList<>();
        String baseName = extractBaseName(currentProduct.getName());
        result.add(currentProduct);
        for (Product product : allProducts) {
            if (product.getId() != currentProduct.getId() &&
                    extractBaseName(product.getName()).equalsIgnoreCase(baseName)) {
                result.add(product);
            }
        }
        return result;
    }

    private String extractBaseName(String fullName) {
        return fullName.replaceAll("\\(.*\\)", "")
                .replaceAll("\".*\"", "")
                .replaceAll(" - .*", "")
                .trim();
    }

    private void setupSameProductsRecyclerView(List<Product> sameProducts) {
        LinearLayoutManager layoutManager = (LinearLayoutManager) sameProductsRecyclerView.getLayoutManager();
        int firstVisiblePosition = layoutManager != null ?
                layoutManager.findFirstVisibleItemPosition() : 0;
        if (sameProductAdapter == null) {
            sameProductAdapter = new SameProductAdapter(sameProducts, this);
            sameProductsRecyclerView.setLayoutManager(new LinearLayoutManager(
                    this, LinearLayoutManager.HORIZONTAL, false));
            while (sameProductsRecyclerView.getItemDecorationCount() > 0) {
                sameProductsRecyclerView.removeItemDecorationAt(0);
            }
            int spacing = getResources().getDimensionPixelSize(R.dimen.same_product_spacing);
            sameProductsRecyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
                @Override
                public void getItemOffsets(Rect outRect, View view,
                                           RecyclerView parent, RecyclerView.State state) {
                    int position = parent.getChildAdapterPosition(view);
                    if (position != parent.getAdapter().getItemCount() - 1) {
                        outRect.right = spacing;
                    }
                    outRect.left = 0;
                }
            });
            sameProductsRecyclerView.setClipToPadding(false);
            sameProductsRecyclerView.setPadding(0, 0, 0, 0);
            sameProductsRecyclerView.setItemAnimator(new DefaultItemAnimator());
            sameProductsRecyclerView.setAdapter(sameProductAdapter);
            sameProductAdapter.setOnItemClickListener(position -> {
                Product selectedProduct = sameProducts.get(position);
                updateProductInfo(selectedProduct);
                sameProductAdapter.setSelectedPosition(position);
            });
        } else {
            sameProductAdapter.updateProducts(sameProducts);
            sameProductsRecyclerView.post(() -> {
                if (firstVisiblePosition >= 0 && firstVisiblePosition < sameProducts.size()) {
                    sameProductsRecyclerView.scrollToPosition(firstVisiblePosition);
                }
            });
        }
    }

    private void loadInitialData() {
        setupProgressBar();
        new Thread(() -> {
            loadHeavyData(true, () -> runOnUiThread(() -> {
                updateBasicProductInfo(currentProduct);
                completeLoading();
            }));
        }).start();
    }

    private void completeLoading() {
        Log.d("ProductInfoActivity", "Completing loading, showing UI");
        topProgressBar.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> {
                    topProgressBar.setVisibility(View.GONE);
                    contentScrollView.setVisibility(View.VISIBLE);
                    contentScrollView.setAlpha(0f);
                    contentScrollView.animate().alpha(1f).setDuration(200).start();
                })
                .start();
    }

    private void updateProductInfo(Product product) {
        currentProduct = product;
        setupProgressBar();
        new Thread(() -> {
            loadHeavyData(false, () -> runOnUiThread(() -> {
                updateBasicProductInfo(currentProduct);
                completeLoading();
            }));
        }).start();
    }

    private void setupSizesRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(
                this, LinearLayoutManager.HORIZONTAL, false);
        sizesRecyclerView.setLayoutManager(layoutManager);
        while (sizesRecyclerView.getItemDecorationCount() > 0) {
            sizesRecyclerView.removeItemDecorationAt(0);
        }
        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.size_item_spacing);
        sizesRecyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                int position = parent.getChildAdapterPosition(view);
                if (position != parent.getAdapter().getItemCount() - 1) {
                    outRect.right = spacingInPixels;
                }
                outRect.left = 0;
            }
        });
        sizesRecyclerView.setClipToPadding(false);
        sizesRecyclerView.setPadding(0, 0, 0, 0);
        sizesRecyclerView.setItemAnimator(new DefaultItemAnimator());
    }

    private void loadSizesForProduct(int productId) {
        SizeContext.loadAllSizesAndProductSizes(this, productId, new SizeContext.AllSizesCallback() {
            @Override
            public void onSuccess(List<Size> allSizes, List<ProductSize> productSizes) {
                runOnUiThread(() -> {
                    try {
                        setupSizesRecyclerView();
                        int storeId = PreferencesHelper.getSelectedStoreId(ProductInfoActivity.this);
                        boolean isStoreSelected = storeId != -1;
                        List<SizeDisplayModel> displaySizes = new ArrayList<>();
                        for (Size size : allSizes) {
                            boolean isAvailable = false;
                            int count = 0;
                            int productSizeId = -1;
                            for (ProductSize ps : productSizes) {
                                if (ps.getSizeId() == size.getId() && (!isStoreSelected || ps.getStoreId() == storeId)) {
                                    isAvailable = ps.getCount() > 0;
                                    count = ps.getCount();
                                    productSizeId = ps.getId();
                                    break;
                                }
                            }
                            displaySizes.add(new SizeDisplayModel(
                                    size.getId(),
                                    size.getValue(),
                                    isAvailable,
                                    isStoreSelected ? count : -1,
                                    productSizeId
                            ));
                        }
                        if (sizeAdapter == null) {
                            sizeAdapter = new SizeAdapter(displaySizes, isStoreSelected, ProductInfoActivity.this);
                            sizesRecyclerView.setAdapter(sizeAdapter);
                        } else {
                            sizeAdapter.updateSizes(displaySizes);
                        }
                        selectFirstAvailableSize(displaySizes, isStoreSelected);
                    } catch (Exception e) {
                        Log.e("ProductInfoActivity", "Ошибка обработки размеров", e);
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e("ProductInfoActivity", "Error loading sizes: " + error);
                runOnUiThread(() -> {
                    Toast.makeText(ProductInfoActivity.this,
                            "Ошибка загрузки размеров: " + error,
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void selectFirstAvailableSize(List<SizeDisplayModel> displaySizes, boolean isStoreSelected) {
        try {
            if (isStoreSelected) {
                for (int i = 0; i < displaySizes.size(); i++) {
                    SizeDisplayModel size = displaySizes.get(i);
                    if (size.isAvailable()) {
                        sizeAdapter.selectedPosition = i;
                        onSizeSelected(size, true);
                        Log.d("ProductInfoActivity", "Selected first available size: " + size.getValue());
                        return;
                    }
                }
                if (!displaySizes.isEmpty()) {
                    sizeAdapter.selectedPosition = -1;
                    onSizeSelected(displaySizes.get(-1), false);
                    Log.d("ProductInfoActivity", "No available sizes, selected first unavailable: " + displaySizes.get(0).getValue());
                } else {
                    onSizeSelected(null, false);
                    Log.d("ProductInfoActivity", "No sizes to select");
                }
            } else {
                if (!displaySizes.isEmpty()) {
                    sizeAdapter.selectedPosition = 0;
                    onSizeSelected(displaySizes.get(0), true);
                    Log.d("ProductInfoActivity", "No store selected, selected first size: " + displaySizes.get(0).getValue());
                } else {
                    onSizeSelected(null, false);
                    Log.d("ProductInfoActivity", "No sizes to select");
                }
            }
        } catch (Exception e) {
            Log.e("ProductInfoActivity", "Error in selectFirstAvailableSize", e);
            onSizeSelected(null, false);
        }
    }

    @Override
    public void onSizeSelected(SizeDisplayModel size, boolean isAvailable) {
        boolean isStoreSelected = PreferencesHelper.getSelectedStoreId(this) != -1;
        if (size != null && isAvailable) {
            String stockText;
            if (isStoreSelected) {
                if (size.getCount() == 1) {
                    stockText = "Осталась последняя пара!";
                    stockCountTextView.setTextColor(Color.RED);
                    stockCountTextView.setTypeface(null, Typeface.BOLD);
                } else if (size.getCount() > 0) {
                    stockText = String.format("В наличии: %d пар(-а)", size.getCount());
                    stockCountTextView.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                    stockCountTextView.setTypeface(null, Typeface.NORMAL);
                } else {
                    stockText = "Нет в наличии";
                    stockCountTextView.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                    stockCountTextView.setTypeface(null, Typeface.NORMAL);
                }
            } else {
                stockText = "В наличии";
                stockCountTextView.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                stockCountTextView.setTypeface(null, Typeface.NORMAL);
            }
            stockCountTextView.setText(stockText);
        } else {
            stockCountTextView.setText("Нет в наличии");
            stockCountTextView.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            stockCountTextView.setTypeface(null, Typeface.NORMAL);
        }
    }

    private void checkFavoriteStatus() {
        if (currentUserId == -1 || currentProduct == null) return;

        FavoriteContext.checkFavorite(currentUserId, String.valueOf(currentProduct.getId()),
                new FavoriteContext.FavoriteCallback() {
                    @Override
                    public void onSuccess(boolean isFavorite) {
                        runOnUiThread(() -> {
                            updateFavoriteIcon(isFavorite);
                        });
                    }

                    @Override
                    public void onError(String error) {
                        Log.e("ProductInfo", "Error checking favorite: " + error);
                    }
                });
    }

    private void toggleFavorite() {
        if (!AuthUtils.isUserLoggedIn(this)) {
            startActivity(new Intent(this, AuthorizationActivity.class));
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            return;
        }
        if (currentUserId == -1 || currentProduct == null) return;
        FavoriteContext.toggleFavorite(currentUserId, String.valueOf(currentProduct.getId()),
                isFavorite, new FavoriteContext.FavoriteCallback() {
                    @Override
                    public void onSuccess(boolean newFavoriteStatus) {
                        runOnUiThread(() -> {
                            isFavorite = newFavoriteStatus;
                            updateFavoriteIcon(isFavorite);
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Toast.makeText(ProductInfoActivity.this,
                                    "Ошибка: " + error,
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    private void updateFavoriteIcon(boolean isFavorite) {
        this.isFavorite = isFavorite;
        if (isFavorite) {
            favoriteIcon.setImageResource(R.drawable.favorite_item_select);
        } else {
            favoriteIcon.setImageResource(R.drawable.favorite_item);
        }
    }

    private void toggleDescription() {
        if (!isInitialized || expandCollapseButton.getVisibility() != View.VISIBLE) {
            Log.d("ProductInfoActivity", "Toggle skipped: not initialized or button invisible");
            return;
        }
        if (isDescriptionExpanded) {
            collapseDescription();
        } else {
            expandDescription();
        }
    }

    private void expandDescription() {
        expandCollapseButton.setText("Скрыть");
        ValueAnimator heightAnimator = ValueAnimator.ofInt(collapsedHeight, expandedHeight);
        heightAnimator.addUpdateListener(animation -> {
            ViewGroup.LayoutParams params = descriptionContainer.getLayoutParams();
            params.height = (int) animation.getAnimatedValue();
            descriptionContainer.setLayoutParams(params);
        });
        ValueAnimator alphaAnimator = ValueAnimator.ofFloat(0.7f, 1f);
        alphaAnimator.addUpdateListener(animation -> {
            descriptionContainer.setAlpha((float) animation.getAnimatedValue());
        });
        heightAnimator.setDuration(400);
        alphaAnimator.setDuration(200);
        heightAnimator.setInterpolator(new DecelerateInterpolator());
        heightAnimator.start();
        alphaAnimator.start();
        descriptionTextView.setMaxLines(Integer.MAX_VALUE);
        descriptionTextView.setEllipsize(null);
        expandCollapseButton.setVisibility(View.VISIBLE);
        isDescriptionExpanded = true;
        Log.d("ProductInfoActivity", "Expanded description, maxLines=MAX, button visible");
    }

    private void collapseDescription() {
        expandCollapseButton.setText("Подробнее");
        ValueAnimator heightAnimator = ValueAnimator.ofInt(expandedHeight, collapsedHeight);
        heightAnimator.addUpdateListener(animation -> {
            ViewGroup.LayoutParams params = descriptionContainer.getLayoutParams();
            params.height = (int) animation.getAnimatedValue();
            descriptionContainer.setLayoutParams(params);
        });
        ValueAnimator alphaAnimator = ValueAnimator.ofFloat(1f, 0.7f);
        alphaAnimator.addUpdateListener(animation -> {
            descriptionContainer.setAlpha((float) animation.getAnimatedValue());
        });
        heightAnimator.setDuration(400);
        alphaAnimator.setDuration(200);
        heightAnimator.setInterpolator(new AccelerateInterpolator());
        heightAnimator.start();
        alphaAnimator.start();
        descriptionTextView.setMaxLines(3);
        descriptionTextView.setEllipsize(TextUtils.TruncateAt.END);
        expandCollapseButton.setVisibility(View.VISIBLE);
        descriptionContainer.requestLayout();
        isDescriptionExpanded = false;
        Log.d("ProductInfoActivity", "Collapsed description, maxLines=3, button visible");
    }

    private void resetDescriptionState() {
        isDescriptionExpanded = false;
        descriptionTextView.setMaxLines(3);
        descriptionTextView.setEllipsize(TextUtils.TruncateAt.END);
        expandCollapseButton.setText("Подробнее");
        expandCollapseButton.setVisibility(View.VISIBLE);
        descriptionContainer.setAlpha(0.7f);
        ViewGroup.LayoutParams params = descriptionContainer.getLayoutParams();
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        descriptionContainer.setLayoutParams(params);
    }

    public void onBack(View view) {
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentProduct != null) {
            loadSizesForProduct(currentProduct.getId());
        }
    }

    public void onAddCart(View view) {
        if (!AuthUtils.isUserLoggedIn(this)) {
            startActivity(new Intent(this, AuthorizationActivity.class));
            return;
        }
        int selectedStoreId = PreferencesHelper.getSelectedStoreId(this);
        if (selectedStoreId == -1) {
            Toast.makeText(this, "Пожалуйста, выберите магазин", Toast.LENGTH_SHORT).show();
            return;
        }
        if (sizeAdapter == null || sizeAdapter.getSelectedSize() == null) {
            Toast.makeText(this, "Пожалуйста, выберите размер", Toast.LENGTH_SHORT).show();
            return;
        }
        SizeDisplayModel selectedSize = sizeAdapter.getSelectedSize();
        if (!selectedSize.isAvailable()) {
            Toast.makeText(this, "Выбранный размер недоступен в этом магазине", Toast.LENGTH_SHORT).show();
            return;
        }
        addToCart(String.valueOf(selectedSize.getProductSizeId()), selectedStoreId);
    }

    private void addToCart(String productSizeId, int storeId) {
        CartContext.getUserBasket(currentUserId, new CartContext.BasketCallback() {
            @Override
            public void onSuccess(String basketId) {
                CartContext.addToBasket(basketId, productSizeId, 1, storeId, new CartContext.AddToBasketCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Toast.makeText(ProductInfoActivity.this,
                                    "Ошибка: " + error, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(ProductInfoActivity.this,
                            "Ошибка: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
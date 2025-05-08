package com.example.sneaker_shop;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.List;

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
    private String currentUserId;
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
    }

    private void setupProgressBar() {
        topProgressBar.setVisibility(View.VISIBLE);
        contentScrollView.setVisibility(View.INVISIBLE);
        topProgressBar.setIndeterminate(true);
    }

    private void loadInitialData() {
        updateBasicProductInfo(currentProduct);
        new Thread(() -> {
            for (int i = 0; i <= 100; i += 5) {
                try {
                    Thread.sleep(30);
                    final int progress = i;
                    runOnUiThread(() -> updateProgress(progress));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            loadHeavyData();
            runOnUiThread(this::completeLoading);
        }).start();
    }

    private void updateProgress(int progress) {
        if (progress < 100) {
            topProgressBar.setProgress(progress);
        }
    }

    private void updateBasicProductInfo(Product product) {
        TextView productNameTextView = findViewById(R.id.productName);
        TextView priceTextView = findViewById(R.id.productPrice);
        productNameTextView.setText(product.getName());
        priceTextView.setText(String.format("₽ %.2f", product.getPrice()));
        descriptionTextView.setText(product.getDescription());
    }

    private void loadHeavyData() {
        runOnUiThread(() -> {
            loadProductImages(currentProduct.getId());
            loadSizesForProduct(currentProduct.getId());
            loadSameProducts();
            checkFavoriteStatus();
            setupDescriptionAnimation();
            setupSizesRecyclerView();
        });
        runOnUiThread(this::setupListeners);
    }

    private void completeLoading() {
        topProgressBar.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction(() -> {
                    topProgressBar.setVisibility(View.GONE);
                    contentScrollView.setVisibility(View.VISIBLE);
                    contentScrollView.setAlpha(0f);
                    contentScrollView.animate().alpha(1f).setDuration(300).start();
                })
                .start();
    }

    private void loadProductImages(int productId) {
        ImageContext.loadImagesForProduct(productId, new ImageContext.ImagesCallback() {
            @Override
            public void onSuccess(List<String> images) {
                runOnUiThread(() -> {
                    productImages.clear();
                    productImages.addAll(images);
                    setupImagesPager();
                    setupImagesIndicator();
                });
            }

            @Override
            public void onError(String error) {
                Log.e("ProductInfo", "Error loading images: " + error);
                runOnUiThread(() -> {
                    productImages.clear();
                    productImages.add("default_image_base64");
                    setupImagesPager();
                });
            }
        });
    }

    private void setupImagesPager() {
        imagesAdapter = new ProductImagesAdapter(productImages);
        productImagesPager.setAdapter(imagesAdapter);
        productImagesPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateImagesIndicator(position);
            }
        });
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
        descriptionContainer.post(() -> {
            descriptionTextView.setMaxLines(3);
            descriptionTextView.setEllipsize(TextUtils.TruncateAt.END);
            collapsedHeight = descriptionContainer.getHeight();
            descriptionTextView.setMaxLines(Integer.MAX_VALUE);
            descriptionTextView.setEllipsize(null);
            descriptionContainer.measure(
                    View.MeasureSpec.makeMeasureSpec(descriptionContainer.getWidth(), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            expandedHeight = descriptionContainer.getMeasuredHeight();
            descriptionTextView.setMaxLines(3);
            descriptionTextView.setEllipsize(TextUtils.TruncateAt.END);
            if (collapsedHeight >= expandedHeight) {
                expandCollapseButton.setVisibility(View.GONE);
                isInitialized = false;
            } else {
                expandCollapseButton.setVisibility(View.VISIBLE);
                isInitialized = true;
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

    private void updateProductInfo(Product product) {
        topProgressBar.setVisibility(View.VISIBLE);
        topProgressBar.setAlpha(1f);
        topProgressBar.setProgress(0);
        updateBasicProductInfo(product);
        currentProduct = product;
        new Thread(() -> {
            for (int i = 0; i <= 100; i += 20) {
                try {
                    Thread.sleep(50);
                    final int progress = i;
                    runOnUiThread(() -> updateProgress(progress));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            runOnUiThread(() -> {
                loadHeavyData();
                topProgressBar.animate()
                        .alpha(0f)
                        .setDuration(300)
                        .withEndAction(() -> topProgressBar.setVisibility(View.GONE))
                        .start();
            });
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
    }

    private void loadSizesForProduct(int productId) {
        SizeContext.loadAllSizesAndProductSizes(productId, new SizeContext.AllSizesCallback() {
            @Override
            public void onSuccess(List<Size> allSizes, List<ProductSize> productSizes) {
                runOnUiThread(() -> {
                    sizeAdapter = new SizeAdapter(allSizes, productSizes, ProductInfoActivity.this);
                    sizesRecyclerView.setAdapter(sizeAdapter);
                    selectFirstAvailableSize(allSizes, productSizes);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(ProductInfoActivity.this,
                            "Ошибка загрузки размеров",
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void selectFirstAvailableSize(List<Size> allSizes, List<ProductSize> productSizes) {
        for (int i = 0; i < allSizes.size(); i++) {
            Size size = allSizes.get(i);
            for (ProductSize ps : productSizes) {
                if (ps.getSizeId() == size.getId() && ps.getCount() > 0) {
                    sizeAdapter.selectedPosition = i;
                    onSizeSelected(size, true);
                    return;
                }
            }
        }
        onSizeSelected(null, false);
    }

    @Override
    public void onSizeSelected(Size size, boolean isAvailable) {
        if (size != null && isAvailable) {
            int stockCount = getStockCountForSize(size.getId(), sizeAdapter.availableProductSizes);
            String stockText;
            if (stockCount == 1) {
                stockText = "Осталась последняя пара!";
                stockCountTextView.setTextColor(Color.RED);
                stockCountTextView.setTypeface(null, Typeface.BOLD);
            } else {
                stockText = String.format("В наличии: %d пар(-а)", stockCount);
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

    private int getStockCountForSize(int sizeId, List<ProductSize> productSizes) {
        for (ProductSize ps : productSizes) {
            if (ps.getSizeId() == sizeId) {
                return ps.getCount();
            }
        }
        return 0;
    }

    private void checkFavoriteStatus() {
        if (currentUserId == null || currentProduct == null) return;

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

        if (currentUserId == null || currentProduct == null) return;

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
        if (!isInitialized || expandCollapseButton.getVisibility() != View.VISIBLE) return;

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
        isDescriptionExpanded = true;
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
        isDescriptionExpanded = false;
    }

    public void onBack(View view) {
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        finish();
    }
}
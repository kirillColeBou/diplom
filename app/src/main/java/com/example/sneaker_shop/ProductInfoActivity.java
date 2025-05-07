package com.example.sneaker_shop;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class ProductInfoActivity extends AppCompatActivity implements SizeAdapter.OnSizeSelectedListener {
    private TextView descriptionTextView;
    private TextView expandCollapseButton;
    private boolean isDescriptionExpanded = false;
    private LinearLayout descriptionContainer;
    private int collapsedHeight;
    private int expandedHeight;
    private boolean isInitialized = false;
    private RecyclerView sizesRecyclerView;
    private TextView stockCountTextView;
    private SizeAdapter sizeAdapter;
    private ImageView favoriteIcon;
    private LinearLayout favoriteButton;
    private boolean isFavorite = false;
    private String currentUserId;
    private Product currentProduct;
    private RecyclerView sameProductsRecyclerView;
    private LinearLayout sameProductsContainer;
    private SameProductAdapter sameProductAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.product_info_activity);
        initViews();
        currentUserId = AuthUtils.getCurrentUserId(this);
        currentProduct = (Product) getIntent().getSerializableExtra("product");
        if (currentProduct != null) {
            loadProductData();
            loadSameProducts();
            loadSizesForProduct(currentProduct.getId());
        }
        checkFavoriteStatus();
        setupListeners();
        setupDescriptionAnimation();
        setupSizesRecyclerView();
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
            isInitialized = true;
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
                sameProductsContainer.setVisibility(View.GONE);
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
        sameProductAdapter = new SameProductAdapter(sameProducts, this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(
                this, LinearLayoutManager.HORIZONTAL, false);
        sameProductsRecyclerView.setLayoutManager(layoutManager);
        int spacing = getResources().getDimensionPixelSize(R.dimen.same_product_spacing);
        sameProductsRecyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect outRect, View view,
                                       RecyclerView parent, RecyclerView.State state) {
                outRect.right = spacing;
            }
        });
        sameProductsRecyclerView.setClipToPadding(false);
        sameProductsRecyclerView.setPadding(spacing, 0, spacing, 0);
        sameProductsRecyclerView.setAdapter(sameProductAdapter);
        sameProductAdapter.setOnItemClickListener(position -> {
            Product selectedProduct = sameProducts.get(position);
            updateProductInfo(selectedProduct);
            sameProductAdapter.setSelectedPosition(position);
        });
    }

    private void updateProductInfo(Product product) {
        TextView productNameTextView = findViewById(R.id.productName);
        TextView priceTextView = findViewById(R.id.productPrice);
        ImageView productImageView = findViewById(R.id.productImage);
        productNameTextView.setText(product.getName());
        priceTextView.setText(String.format("₽ %.2f", product.getPrice()));
        if (product.getImage() != null && !product.getImage().isEmpty()) {
            try {
                String base64Image = product.getImage().split(",")[1];
                byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                productImageView.setImageBitmap(decodedByte);
            } catch (Exception e) {
                productImageView.setImageResource(R.drawable.nike_air_force);
            }
        }
        currentProduct = product;
        loadSizesForProduct(product.getId());
        checkFavoriteStatus();
    }

    private void setupSizesRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(
                this, LinearLayoutManager.HORIZONTAL, false);
        sizesRecyclerView.setLayoutManager(layoutManager);
        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.size_item_spacing);
        sizesRecyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect outRect, View view,
                                       RecyclerView parent, RecyclerView.State state) {
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

    private void loadProductData() {
        currentProduct = (Product) getIntent().getSerializableExtra("product");
        if (currentProduct != null) {
            Product product = (Product) getIntent().getSerializableExtra("product");
            if (product != null) {
                TextView productNameTextView = findViewById(R.id.productName);
                TextView priceTextView = findViewById(R.id.productPrice);
                ImageView productImageView = findViewById(R.id.productImage);
                productNameTextView.setText(product.getName());
                priceTextView.setText(String.format("₽ %.2f", product.getPrice()));
                descriptionTextView.setText(product.getDescription());
                if (product.getImage() != null && !product.getImage().isEmpty()) {
                    try {
                        String base64Image = product.getImage().split(",")[1];
                        byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        productImageView.setImageBitmap(decodedByte);
                    } catch (Exception e) {
                        productImageView.setImageResource(R.drawable.nike_air_force);
                    }
                }
            }
        }
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
        if (!isInitialized) return;
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
package com.example.sneaker_shop;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.util.ArrayList;
import java.util.List;

public class FavoriteActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private FavoriteAdapter adapter;
    private List<Product> favoriteProducts = new ArrayList<>();
    private SwipeRefreshLayout swipeRefreshLayout;
    private String currentUserId;
    private BottomSheetDialog bottomSheetDialog;
    private int currentSelectedPosition = -1;
    private LinearLayout emptyFavoriteLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!AuthUtils.isUserLoggedIn(this)) {
            startActivity(new Intent(this, AuthorizationActivity.class));
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            finish();
            return;
        }
        setContentView(R.layout.favorite_activity);
        currentUserId = AuthUtils.getCurrentUserId(this);
        emptyFavoriteLayout = findViewById(R.id.empty_favorite);
        initViews();
        setupRecyclerView();
        setupBottomSheet();
        loadFavoriteProducts();
    }

    private void checkEmptyState() {
        if (favoriteProducts.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyFavoriteLayout.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyFavoriteLayout.setVisibility(View.GONE);
        }
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recycler_view_favorite);
        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        swipeRefreshLayout.setOnRefreshListener(this::loadFavoriteProducts);
        swipeRefreshLayout.setProgressViewOffset(false, 0, 100);
        swipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
        );
    }

    private void setupBottomSheet() {
        bottomSheetDialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_remove_favorite, null);
        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.getWindow().setDimAmount(0.6f);
        LinearLayout removeLayout = bottomSheetView.findViewById(R.id.remove);
        removeLayout.setOnClickListener(v -> removeFromFavorites());
        bottomSheetDialog.setOnShowListener(dialog -> {
            View bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setSkipCollapsed(true);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                bottomSheet.setAlpha(0f);
                bottomSheet.animate()
                        .alpha(1f)
                        .setDuration(300)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
            }
        });
    }

    private void showBottomSheet(int position) {
        currentSelectedPosition = position;
        if (bottomSheetDialog != null && !bottomSheetDialog.isShowing()) {
            bottomSheetDialog.show();
        }
    }

    private void removeFromFavorites() {
        if (currentSelectedPosition < 0 || currentSelectedPosition >= favoriteProducts.size()) {
            return;
        }
        Product product = favoriteProducts.get(currentSelectedPosition);
        View bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            bottomSheet.animate()
                    .alpha(0f)
                    .setDuration(250)
                    .setInterpolator(new AccelerateInterpolator())
                    .withEndAction(() -> {
                        bottomSheetDialog.dismiss();
                        performFavoriteRemoval(product, currentSelectedPosition);
                    })
                    .start();
        } else {
            bottomSheetDialog.dismiss();
            performFavoriteRemoval(product, currentSelectedPosition);
        }
    }

    private void performFavoriteRemoval(Product product, int position) {
        FavoriteContext.toggleFavorite(
                currentUserId,
                String.valueOf(product.getId()),
                true,
                new FavoriteContext.FavoriteCallback() {
                    @Override
                    public void onSuccess(boolean isFavorite) {
                        runOnUiThread(() -> {
                            if (!isFavorite) {
                                int actualPosition = favoriteProducts.indexOf(product);
                                if (actualPosition != -1) {
                                    favoriteProducts.remove(actualPosition);
                                    adapter.notifyItemRemoved(actualPosition);
                                }
                                checkEmptyState();
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Toast.makeText(FavoriteActivity.this,
                                    "Ошибка: " + error,
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    private void setupRecyclerView() {
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(layoutManager);
        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.favorite_item_spacing);
        recyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                int halfSpacing = spacingInPixels / 2;
                outRect.left = halfSpacing;
                outRect.right = halfSpacing;
                outRect.bottom = spacingInPixels;
                if (parent.getChildAdapterPosition(view) < 2) {
                    outRect.top = spacingInPixels;
                }
            }
        });
        recyclerView.setClipToPadding(false);
        recyclerView.setPadding(spacingInPixels, 0, spacingInPixels, 0);
        adapter = new FavoriteAdapter(favoriteProducts, this, position -> {
            showBottomSheet(position);
        });
        recyclerView.setAdapter(adapter);
    }

    private void loadFavoriteProducts() {
        swipeRefreshLayout.setRefreshing(true);
        FavoriteContext.loadFavorites(currentUserId, new FavoriteContext.FavoritesCallback() {
            @Override
            public void onSuccess(List<Product> products) {
                runOnUiThread(() -> {
                    favoriteProducts.clear();
                    favoriteProducts.addAll(products);
                    adapter.notifyDataSetChanged();
                    swipeRefreshLayout.setRefreshing(false);
                    checkEmptyState();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(FavoriteActivity.this,
                            "Ошибка загрузки избранного",
                            Toast.LENGTH_SHORT).show();
                    swipeRefreshLayout.setRefreshing(false);
                    Log.e("FavoriteActivity", error);
                });
            }
        });
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

    public void onCart(View view) {
        startActivity(new Intent(this, CartActivity.class));
        overridePendingTransition(0, 0);
        finish();
    }

    public void onPerson(View view) {
        startActivity(new Intent(this, PersonActivity.class));
        overridePendingTransition(0, 0);
        finish();
    }
}
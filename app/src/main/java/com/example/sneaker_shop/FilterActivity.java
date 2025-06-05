package com.example.sneaker_shop;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.IntStream;

public class FilterActivity extends AppCompatActivity {
    private EditText minPriceEditText;
    private EditText maxPriceEditText;
    private RecyclerView shoeColorRecyclerView;
    private RecyclerView soleColorRecyclerView;
    private RecyclerView brandRecyclerView;
    private RecyclerView categoryRecyclerView;
    private RecyclerView sizeRecyclerView;
    private ShoeColorAdapter shoeColorAdapter;
    private SoleColorAdapter soleColorAdapter;
    private BrandAdapter brandAdapter;
    private FilterCategoryAdapter categoryAdapter;
    private FilterSizeAdapter sizeAdapter;
    private List<Colors> shoeColors = new ArrayList<>();
    private List<Colors> soleColors = new ArrayList<>();
    private List<Brands> brands = new ArrayList<>();
    private List<Category> categories = new ArrayList<>();
    private List<Size> sizes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.filter_activity);
        minPriceEditText = findViewById(R.id.minPrice);
        maxPriceEditText = findViewById(R.id.maxPrice);
        shoeColorRecyclerView = findViewById(R.id.shoeColorRecyclerView);
        soleColorRecyclerView = findViewById(R.id.soleColorRecyclerView);
        brandRecyclerView = findViewById(R.id.brandRecyclerView);
        categoryRecyclerView = findViewById(R.id.categoryRecyclerView);
        sizeRecyclerView = findViewById(R.id.sizeRecyclerView);
        shoeColorAdapter = new ShoeColorAdapter(shoeColors, this, position -> {});
        soleColorAdapter = new SoleColorAdapter(soleColors, this, position -> {});
        brandAdapter = new BrandAdapter(brands, this, position -> {});
        categoryAdapter = new FilterCategoryAdapter(categories, position -> {});
        sizeAdapter = new FilterSizeAdapter(sizes, position -> {});
        shoeColorRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        soleColorRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        brandRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        categoryRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        sizeRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        shoeColorRecyclerView.setAdapter(shoeColorAdapter);
        soleColorRecyclerView.setAdapter(soleColorAdapter);
        brandRecyclerView.setAdapter(brandAdapter);
        categoryRecyclerView.setAdapter(categoryAdapter);
        sizeRecyclerView.setAdapter(sizeAdapter);
        loadFilterData();
    }

    private void loadFilterData() {
        CountDownLatch latch = new CountDownLatch(5);
        FilterContext.loadColors(new FilterContext.FilterCallback() {
            @Override
            public void onSuccess(List<Colors> loadedColors) {
                shoeColors.clear();
                shoeColors.addAll(loadedColors);
                runOnUiThread(() -> shoeColorAdapter.notifyDataSetChanged());
                latch.countDown();
            }
            @Override
            public void onSuccessBrands(List<Brands> brands) {}
            @Override
            public void onSuccessCategories(List<Category> categories) {}
            @Override
            public void onSuccessSizes(List<Size> sizes) {}
            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(FilterActivity.this, "Ошибка загрузки цветов обуви: " + error, Toast.LENGTH_SHORT).show());
                latch.countDown();
            }
        });
        FilterContext.loadColors(new FilterContext.FilterCallback() {
            @Override
            public void onSuccess(List<Colors> loadedColors) {
                soleColors.clear();
                soleColors.addAll(loadedColors);
                runOnUiThread(() -> soleColorAdapter.notifyDataSetChanged());
                latch.countDown();
            }
            @Override
            public void onSuccessBrands(List<Brands> brands) {}
            @Override
            public void onSuccessCategories(List<Category> categories) {}
            @Override
            public void onSuccessSizes(List<Size> sizes) {}
            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(FilterActivity.this, "Ошибка загрузки цветов подошвы: " + error, Toast.LENGTH_SHORT).show());
                latch.countDown();
            }
        });

        FilterContext.loadBrands(new FilterContext.FilterCallback() {
            @Override
            public void onSuccess(List<Colors> colors) {}
            @Override
            public void onSuccessBrands(List<Brands> loadedBrands) {
                brands.clear();
                brands.addAll(loadedBrands);
                runOnUiThread(() -> brandAdapter.notifyDataSetChanged());
                latch.countDown();
            }
            @Override
            public void onSuccessCategories(List<Category> categories) {}
            @Override
            public void onSuccessSizes(List<Size> sizes) {}
            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(FilterActivity.this, "Ошибка загрузки брендов: " + error, Toast.LENGTH_SHORT).show());
                latch.countDown();
            }
        });

        FilterContext.loadCategories(new FilterContext.FilterCallback() {
            @Override
            public void onSuccess(List<Colors> colors) {}
            @Override
            public void onSuccessBrands(List<Brands> brands) {}
            @Override
            public void onSuccessCategories(List<Category> loadedCategories) {
                categories.clear();
                categories.addAll(loadedCategories);
                runOnUiThread(() -> categoryAdapter.notifyDataSetChanged());
                latch.countDown();
            }
            @Override
            public void onSuccessSizes(List<Size> sizes) {}
            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(FilterActivity.this, "Ошибка загрузки категорий: " + error, Toast.LENGTH_SHORT).show());
                latch.countDown();
            }
        });

        FilterContext.loadSizes(new FilterContext.FilterCallback() {
            @Override
            public void onSuccess(List<Colors> colors) {}
            @Override
            public void onSuccessBrands(List<Brands> brands) {}
            @Override
            public void onSuccessCategories(List<Category> categories) {}
            @Override
            public void onSuccessSizes(List<Size> loadedSizes) {
                sizes.clear();
                sizes.addAll(loadedSizes);
                runOnUiThread(() -> sizeAdapter.notifyDataSetChanged());
                latch.countDown();
            }
            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(FilterActivity.this, "Ошибка загрузки размеров: " + error, Toast.LENGTH_SHORT).show());
                latch.countDown();
            }
        });

        new Thread(() -> {
            try {
                latch.await();
                runOnUiThread(this::restoreFilterState);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void restoreFilterState() {
        Intent intent = getIntent();
        double minPrice = intent.getDoubleExtra("minPrice", 0.0);
        double maxPrice = intent.getDoubleExtra("maxPrice", 100000.0);
        int[] brandIds = intent.getIntArrayExtra("brandIds");
        int[] shoeColorIds = intent.getIntArrayExtra("shoeColorIds");
        int[] soleColorIds = intent.getIntArrayExtra("soleColorIds");
        int[] categoryIds = intent.getIntArrayExtra("categoryIds");
        int[] sizeIds = intent.getIntArrayExtra("sizeIds");
        if (intent.hasExtra("minPrice") && minPrice != 0.0) {
            minPriceEditText.setText(String.valueOf(minPrice));
        }
        if (intent.hasExtra("maxPrice") && maxPrice != 100000.0) {
            maxPriceEditText.setText(String.valueOf(maxPrice));
        }
        if (shoeColorIds != null) shoeColorAdapter.setSelectedIds(shoeColorIds);
        if (soleColorIds != null) soleColorAdapter.setSelectedIds(soleColorIds);
        if (brandIds != null) brandAdapter.setSelectedIds(brandIds);
        if (categoryIds != null) categoryAdapter.setSelectedIds(categoryIds);
        if (sizeIds != null) sizeAdapter.setSelectedIds(sizeIds);
    }

    public void onApplyFilter(View view) {
        double minPrice = minPriceEditText.getText().toString().isEmpty() ? 0.0 : Double.parseDouble(minPriceEditText.getText().toString());
        double maxPrice = maxPriceEditText.getText().toString().isEmpty() ? 100000.0 : Double.parseDouble(maxPriceEditText.getText().toString());
        List<Colors> selectedShoeColors = shoeColorAdapter.getSelectedColors();
        List<Colors> selectedSoleColors = soleColorAdapter.getSelectedColors();
        List<Brands> selectedBrands = brandAdapter.getSelectedBrands();
        List<Category> selectedCategories = categoryAdapter.getSelectedCategories();
        List<Size> selectedSizes = sizeAdapter.getSelectedSizes();
        Intent intent = new Intent(this, FilterResultActivity.class);
        intent.putExtra("minPrice", minPrice);
        intent.putExtra("maxPrice", maxPrice);
        int[] brandIds = selectedBrands.stream().mapToInt(Brands::getId).toArray();
        intent.putExtra("brandIds", brandIds);
        int[] shoeColorIds = selectedShoeColors.stream().mapToInt(Colors::getId).toArray();
        intent.putExtra("shoeColorIds", shoeColorIds);
        int[] soleColorIds = selectedSoleColors.stream().mapToInt(Colors::getId).toArray();
        intent.putExtra("soleColorIds", soleColorIds);
        int[] categoryIds = selectedCategories.stream().mapToInt(Category::getId).toArray();
        intent.putExtra("categoryIds", categoryIds);
        int[] sizeIds = selectedSizes.stream().mapToInt(Size::getId).toArray();
        intent.putExtra("sizeIds", sizeIds);
        int storeId = PreferencesHelper.getSelectedStoreId(this);
        intent.putExtra("storeId", storeId);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    public void onBack(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        double minPrice = minPriceEditText.getText().toString().isEmpty() ? 0.0 : Double.parseDouble(minPriceEditText.getText().toString());
        double maxPrice = maxPriceEditText.getText().toString().isEmpty() ? 100000.0 : Double.parseDouble(maxPriceEditText.getText().toString());
        int[] brandIds = brandAdapter.getSelectedBrands().stream().mapToInt(Brands::getId).toArray();
        int[] shoeColorIds = shoeColorAdapter.getSelectedColors().stream().mapToInt(Colors::getId).toArray();
        int[] soleColorIds = soleColorAdapter.getSelectedColors().stream().mapToInt(Colors::getId).toArray();
        int[] categoryIds = categoryAdapter.getSelectedCategories().stream().mapToInt(Category::getId).toArray();
        int[] sizeIds = sizeAdapter.getSelectedSizes().stream().mapToInt(Size::getId).toArray();
        int storeId = PreferencesHelper.getSelectedStoreId(this);
        intent.putExtra("minPrice", minPrice);
        intent.putExtra("maxPrice", maxPrice);
        intent.putExtra("brandIds", brandIds);
        intent.putExtra("shoeColorIds", shoeColorIds);
        intent.putExtra("soleColorIds", soleColorIds);
        intent.putExtra("categoryIds", categoryIds);
        intent.putExtra("sizeIds", sizeIds);
        intent.putExtra("storeId", storeId);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }
}
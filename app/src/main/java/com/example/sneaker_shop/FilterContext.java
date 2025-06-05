package com.example.sneaker_shop;

import android.os.AsyncTask;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import java.util.ArrayList;
import java.util.List;

public class FilterContext {

    private static final String COLORS_URL = "https://mgxymxiehfsptuubuqfv.supabase.co/rest/v1/colors";
    private static final String BRANDS_URL = "https://mgxymxiehfsptuubuqfv.supabase.co/rest/v1/brands";
    private static final String CATEGORIES_URL = "https://mgxymxiehfsptuubuqfv.supabase.co/rest/v1/categories";
    private static final String SIZES_URL = "https://mgxymxiehfsptuubuqfv.supabase.co/rest/v1/sizes";

    public interface FilterCallback {
        void onSuccess(List<Colors> colors);
        void onSuccessBrands(List<Brands> brands);
        void onSuccessCategories(List<Category> categories);
        void onSuccessSizes(List<Size> sizes);
        void onError(String error);
    }

    public static void loadColors(FilterCallback callback) {
        new LoadColorsTask(callback).execute();
    }

    public static void loadBrands(FilterCallback callback) {
        new LoadBrandsTask(callback).execute();
    }

    public static void loadCategories(FilterCallback callback) {
        new LoadCategoriesTask(callback).execute();
    }

    public static void loadSizes(FilterCallback callback) {
        new LoadSizesTask(callback).execute();
    }

    private static class LoadColorsTask extends AsyncTask<Void, Void, List<Colors>> {
        private final FilterCallback callback;
        private String error;

        LoadColorsTask(FilterCallback callback) {
            this.callback = callback;
        }

        @Override
        protected List<Colors> doInBackground(Void... voids) {
            try {
                String url = COLORS_URL + "?select=id,name";
                String response = Jsoup.connect(url)
                        .header("Authorization", UserContext.TOKEN())
                        .header("apikey", UserContext.SECRET())
                        .ignoreContentType(true)
                        .get()
                        .body()
                        .text();
                JSONArray colorsArray = new JSONArray(response);
                List<Colors> colors = new ArrayList<>();
                for (int i = 0; i < colorsArray.length(); i++) {
                    JSONObject colorObj = colorsArray.getJSONObject(i);
                    colors.add(new Colors(colorObj.getInt("id"), colorObj.getString("name")));
                }
                return colors;
            } catch (Exception e) {
                error = "Error: " + e.getMessage();
                Log.e("FilterContext", error, e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<Colors> colors) {
            if (error != null) {
                callback.onError(error);
            } else if (colors != null) {
                callback.onSuccess(colors);
            } else {
                callback.onError("Failed to load colors");
            }
        }
    }

    private static class LoadBrandsTask extends AsyncTask<Void, Void, List<Brands>> {
        private final FilterCallback callback;
        private String error;

        LoadBrandsTask(FilterCallback callback) {
            this.callback = callback;
        }

        @Override
        protected List<Brands> doInBackground(Void... voids) {
            try {
                String url = BRANDS_URL + "?select=id,name";
                String response = Jsoup.connect(url)
                        .header("Authorization", UserContext.TOKEN())
                        .header("apikey", UserContext.SECRET())
                        .ignoreContentType(true)
                        .get()
                        .body()
                        .text();
                JSONArray brandsArray = new JSONArray(response);
                List<Brands> brands = new ArrayList<>();
                for (int i = 0; i < brandsArray.length(); i++) {
                    JSONObject brandObj = brandsArray.getJSONObject(i);
                    brands.add(new Brands(brandObj.getInt("id"), brandObj.getString("name")));
                }
                return brands;
            } catch (Exception e) {
                error = "Error: " + e.getMessage();
                Log.e("FilterContext", error, e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<Brands> brands) {
            if (error != null) {
                callback.onError(error);
            } else if (brands != null) {
                callback.onSuccessBrands(brands);
            } else {
                callback.onError("Failed to load brands");
            }
        }
    }

    private static class LoadCategoriesTask extends AsyncTask<Void, Void, List<Category>> {
        private final FilterCallback callback;
        private String error;

        LoadCategoriesTask(FilterCallback callback) {
            this.callback = callback;
        }

        @Override
        protected List<Category> doInBackground(Void... voids) {
            try {
                String url = CATEGORIES_URL + "?select=id,name";
                String response = Jsoup.connect(url)
                        .header("Authorization", UserContext.TOKEN())
                        .header("apikey", UserContext.SECRET())
                        .ignoreContentType(true)
                        .get()
                        .body()
                        .text();
                JSONArray categoriesArray = new JSONArray(response);
                List<Category> categories = new ArrayList<>();
                for (int i = 0; i < categoriesArray.length(); i++) {
                    JSONObject categoryObj = categoriesArray.getJSONObject(i);
                    categories.add(new Category(categoryObj.getInt("id"), categoryObj.getString("name"), false));
                }
                return categories;
            } catch (Exception e) {
                error = "Error: " + e.getMessage();
                Log.e("FilterContext", error, e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<Category> categories) {
            if (error != null) {
                callback.onError(error);
            } else if (categories != null) {
                callback.onSuccessCategories(categories);
            } else {
                callback.onError("Failed to load categories");
            }
        }
    }

    private static class LoadSizesTask extends AsyncTask<Void, Void, List<Size>> {
        private final FilterCallback callback;
        private String error;

        LoadSizesTask(FilterCallback callback) {
            this.callback = callback;
        }

        @Override
        protected List<Size> doInBackground(Void... voids) {
            try {
                String url = SIZES_URL + "?select=id,value";
                String response = Jsoup.connect(url)
                        .header("Authorization", UserContext.TOKEN())
                        .header("apikey", UserContext.SECRET())
                        .ignoreContentType(true)
                        .get()
                        .body()
                        .text();
                JSONArray sizesArray = new JSONArray(response);
                List<Size> sizes = new ArrayList<>();
                for (int i = 0; i < sizesArray.length(); i++) {
                    JSONObject sizeObj = sizesArray.getJSONObject(i);
                    sizes.add(new Size(sizeObj.getInt("id"), sizeObj.getString("value")));
                }
                return sizes;
            } catch (Exception e) {
                error = "Error: " + e.getMessage();
                Log.e("FilterContext", error, e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<Size> sizes) {
            if (error != null) {
                callback.onError(error);
            } else if (sizes != null) {
                callback.onSuccessSizes(sizes);
            } else {
                callback.onError("Failed to load sizes");
            }
        }
    }
}
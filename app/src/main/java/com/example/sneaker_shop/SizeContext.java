package com.example.sneaker_shop;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.util.ArrayList;
import java.util.List;

public class SizeContext {
    private static final String URL = "https://mgxymxiehfsptuubuqfv.supabase.co/rest/v1/";
    public interface AllSizesCallback {
        void onSuccess(List<Size> allSizes, List<ProductSize> productSizes);
        void onError(String error);
    }

    public static void loadAllSizesAndProductSizes(Context context, int productId, AllSizesCallback callback) {
        new LoadAllSizesTask(context, productId, callback).execute();
    }

    private static class LoadAllSizesTask extends AsyncTask<Void, Void, Pair<List<Size>, List<ProductSize>>> {
        private final Context context;
        private final int productId;
        private final AllSizesCallback callback;
        private String error;

        LoadAllSizesTask(Context context, int productId, AllSizesCallback callback) {
            this.context = context;
            this.productId = productId;
            this.callback = callback;
        }

        @Override
        protected Pair<List<Size>, List<ProductSize>> doInBackground(Void... voids) {
            try {
                String allSizesUrl = URL + "sizes?order=value.asc";
                Document allSizesDoc = Jsoup.connect(allSizesUrl)
                        .header("apikey", UserContext.SECRET())
                        .header("Authorization", UserContext.TOKEN())
                        .ignoreContentType(true)
                        .get();
                JSONArray allSizesArray = new JSONArray(allSizesDoc.body().text());
                List<Size> allSizes = new ArrayList<>();
                for (int i = 0; i < allSizesArray.length(); i++) {
                    JSONObject obj = allSizesArray.getJSONObject(i);
                    allSizes.add(new Size(obj.getInt("id"), obj.getString("value")));
                }
                int selectedStoreId = PreferencesHelper.getSelectedStoreId(context);
                String productSizesUrl;

                if (selectedStoreId != -1) {
                    productSizesUrl = URL + "product_size?product_id=eq." + productId +
                            "&store_id=eq." + selectedStoreId + "&select=id,product_id,size_id,count,store_id";
                } else {
                    productSizesUrl = URL + "product_size?product_id=eq." + productId +
                            "&select=id,product_id,size_id,count,store_id";
                }
                Document productSizesDoc = Jsoup.connect(productSizesUrl)
                        .header("apikey", UserContext.SECRET())
                        .header("Authorization", UserContext.TOKEN())
                        .ignoreContentType(true)
                        .get();
                JSONArray productSizesArray = new JSONArray(productSizesDoc.body().text());
                List<ProductSize> productSizes = new ArrayList<>();
                for (int i = 0; i < productSizesArray.length(); i++) {
                    JSONObject obj = productSizesArray.getJSONObject(i);
                    productSizes.add(new ProductSize(
                            obj.getInt("id"),
                            obj.getInt("product_id"),
                            obj.getInt("size_id"),
                            obj.getInt("count"),
                            obj.optInt("store_id", -1)
                    ));
                }
                return new Pair<>(allSizes, productSizes);
            } catch (Exception e) {
                error = "Error loading sizes: " + e.getMessage();
                Log.e("SizeContext", error, e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Pair<List<Size>, List<ProductSize>> result) {
            if (error != null) {
                callback.onError(error);
            } else if (result != null) {
                callback.onSuccess(result.first, result.second);
            } else {
                callback.onError("Unknown error occurred");
            }
        }
    }
}
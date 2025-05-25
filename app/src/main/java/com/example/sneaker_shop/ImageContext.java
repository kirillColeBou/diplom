package com.example.sneaker_shop;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.util.ArrayList;
import java.util.List;

public class ImageContext {
    private static final String URL = "https://mgxymxiehfsptuubuqfv.supabase.co/rest/v1/images";
    private static final String TOKEN = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im1neHlteGllaGZzcHR1dWJ1cWZ2Iiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc0NTIyNzY0NSwiZXhwIjoyMDYwODAzNjQ1fQ.LNqLc1o8I8eZUxYuFXknXZZhzN5kRh0eggmg5tItiM0";
    private static final String SECRET = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im1neHlteGllaGZzcHR1dWJ1cWZ2Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDUyMjc2NDUsImV4cCI6MjA2MDgwMzY0NX0.QXcy5Dpd4_b58-xfpvPAIgm9U8Pj6w62RW6p7NDUKyQ";

    public interface ImagesCallback {
        void onSuccess(List<String> images);
        void onError(String error);
    }

    public static void loadImagesForProduct(Context context, int productId, ImagesCallback callback) {
        new LoadImagesTask(context, productId, callback).execute();
    }

    private static class LoadImagesTask extends AsyncTask<Void, Void, List<String>> {
        private final Context context;
        private final int productId;
        private final ImagesCallback callback;
        private String error;

        LoadImagesTask(Context context, int productId, ImagesCallback callback) {
            this.context = context;
            this.productId = productId;
            this.callback = callback;
        }

        @Override
        protected List<String> doInBackground(Void... voids) {
            ImageCacheManager cacheManager = ImageCacheManager.getInstance(context);
            String cachePrefix = "product_" + productId;
            List<String> cachedImages = cacheManager.getAllBase64FromDiskCache(cachePrefix);
            if (!cachedImages.isEmpty()) {
                return cachedImages;
            }
            try {
                String url = URL + "?product_id=eq." + productId + "&select=images_byte64";
                Document doc = Jsoup.connect(url)
                        .header("Authorization", TOKEN)
                        .header("apikey", SECRET)
                        .ignoreContentType(true)
                        .get();
                JSONArray jsonArray = new JSONArray(doc.body().text());
                List<String> images = new ArrayList<>();
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    String imageBase64 = obj.getString("images_byte64");
                    if (!imageBase64.isEmpty()) {
                        images.add(imageBase64);
                    }
                }
                if (!images.isEmpty()) {
                    cacheManager.cacheAllImages(cachePrefix, images);
                }
                return images;
            } catch (Exception e) {
                error = "Error loading images: " + e.getMessage();
                Log.e("Supabase", error, e);
                return new ArrayList<>();
            }
        }

        @Override
        protected void onPostExecute(List<String> images) {
            if (error != null) {
                callback.onError(error);
            } else {
                callback.onSuccess(images);
            }
        }
    }
}
package com.example.sneaker_shop;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.util.LruCache;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ImageCacheManager {
    private static ImageCacheManager instance;
    private final LruCache<String, Bitmap> memoryCache;
    private final File diskCacheDir;
    private static final String CACHE_DIR = "image_cache";

    private ImageCacheManager(Context context) {
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = maxMemory / 8;
        memoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };

        diskCacheDir = new File(context.getCacheDir(), CACHE_DIR);
        if (!diskCacheDir.exists()) {
            diskCacheDir.mkdirs();
        }
    }

    public static synchronized ImageCacheManager getInstance(Context context) {
        if (instance == null) {
            instance = new ImageCacheManager(context.getApplicationContext());
        }
        return instance;
    }

    public Bitmap getBitmapFromMemoryCache(String key) {
        return memoryCache.get(key);
    }

    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemoryCache(key) == null) {
            memoryCache.put(key, bitmap);
        }
    }

    public Bitmap getBitmapFromDiskCache(String key) {
        File file = new File(diskCacheDir, key.hashCode() + ".png");
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                return BitmapFactory.decodeStream(fis);
            } catch (IOException e) {
                Log.e("ImageCacheManager", "Error from getBitmapFromDiskCache", e);
            }
        }
        return null;
    }

    public void addBitmapToDiskCache(String key, Bitmap bitmap) {
        File file = new File(diskCacheDir, key.hashCode() + ".png");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (IOException e) {
            Log.e("ImageCacheManager", "Error addBitmapToDiskCache", e);
        }
    }

    public void addBase64ToDiskCache(String key, String base64) {
        File file = new File(diskCacheDir, key.hashCode() + ".txt");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(base64.getBytes());
        } catch (IOException e) {
            Log.e("ImageCacheManager", "Error from addBase64ToDiskCache", e);
        }
    }

    public List<String> getAllBase64FromDiskCache(String prefix) {
        List<String> result = new ArrayList<>();
        File[] files = diskCacheDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().startsWith(prefix.hashCode() + "_")) {
                    try (FileInputStream fis = new FileInputStream(file)) {
                        byte[] bytes = new byte[(int) file.length()];
                        fis.read(bytes);
                        result.add(new String(bytes));
                    } catch (IOException e) {
                        Log.e("ImageCacheManager", "Error from getAllBase64FromDiskCache", e);
                    }
                }
            }
        }
        return result;
    }

    public void cacheAllImages(String prefix, List<String> images) {
        for (int i = 0; i < images.size(); i++) {
            addBase64ToDiskCache(prefix + "_" + i, images.get(i));
        }
    }
}
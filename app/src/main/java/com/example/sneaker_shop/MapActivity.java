package com.example.sneaker_shop;

import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.yandex.mapkit.Animation;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.IconStyle;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.runtime.image.ImageProvider;
import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class MapActivity extends AppCompatActivity {
    private MapView mapView;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private List<Stores> stores = new ArrayList<>();
    private YandexGeocoderApi geocoderApi;
    private static final String GEOCODER_API_KEY = "a50c9598-e32d-4182-a4a6-f0827d853577";
    private static boolean isMapKitInitialized = false;
    private BottomSheetDialog bottomSheetDialog;
    private Stores selectedStore;
    private View bottomSheetView;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private boolean locationFound = false;
    private static final int LOCATION_SETTINGS_REQUEST_CODE = 1002;

    interface YandexGeocoderApi {
        @GET("1.x/")
        Call<GeocoderResponse> geocode(
                @Query("apikey") String apiKey,
                @Query("geocode") String address,
                @Query("format") String format
        );
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (!isMapKitInitialized) {
            MapKitFactory.setApiKey("c8f9f946-028c-44ba-ba67-fbf77c6ea20a");
            MapKitFactory.initialize(this);
            isMapKitInitialized = true;
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_activity);
        mapView = findViewById(R.id.mapview);
        mapView.getMap().move(
                new CameraPosition(new Point(55.751244, 37.618423), 10.0f, 0.0f, 0.0f),
                new Animation(Animation.Type.SMOOTH, 0),
                null
        );
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://geocode-maps.yandex.ru/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        geocoderApi = retrofit.create(YandexGeocoderApi.class);
        loadStoresAndSetupMap();
        setupBottomSheet();
        checkLocationPermission();
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            checkLocationSettings();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void checkLocationSettings() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());
        task.addOnSuccessListener(this, locationSettingsResponse -> {
            getLastKnownLocation();
        });
        task.addOnFailureListener(this, e -> {
            if (e instanceof ResolvableApiException) {
                try {
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(this, LOCATION_SETTINGS_REQUEST_CODE);
                } catch (IntentSender.SendIntentException sendEx) {
                    Log.e("MapActivity", "Error showing location settings resolution", sendEx);
                    showDefaultLocation();
                }
            } else {
                showDefaultLocation();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == LOCATION_SETTINGS_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                getLastKnownLocation();
            } else {
                showDefaultLocation();
                Toast.makeText(this, "Для определения местоположения включите GPS", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void getLastKnownLocation() {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null && !locationFound) {
                            Log.d("MapActivity", "Last known location found: " + location);
                            locationFound = true;
                            Point userLocation = new Point(location.getLatitude(), location.getLongitude());
                            moveCameraToLocation(userLocation);
                        } else {
                            Log.d("MapActivity", "Last known location is null, requesting updates");
                            requestLocationUpdates();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("MapActivity", "Error getting last location", e);
                        requestLocationUpdates();
                    });
        } catch (SecurityException e) {
            Log.e("MapActivity", "SecurityException", e);
        }
    }

    private void requestLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setNumUpdates(3);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null || locationFound) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        locationFound = true;
                        Point userLocation = new Point(location.getLatitude(), location.getLongitude());
                        if (isValidLocation(userLocation)) {
                            moveCameraToLocation(userLocation);
                        } else {
                            Log.w("MapActivity", "Invalid location received: " + location.getLatitude() + ", " + location.getLongitude());
                            showDefaultLocation();
                        }
                        fusedLocationClient.removeLocationUpdates(this);
                        break;
                    }
                }
            }
        };
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest,
                    locationCallback,
                    Looper.getMainLooper());
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (!locationFound) {
                    Log.d("MapActivity", "Location request timed out");
                    fusedLocationClient.removeLocationUpdates(locationCallback);
                    showDefaultLocation();
                    Toast.makeText(MapActivity.this, "Не удалось определить местоположение. Проверьте GPS и интернет-соединение.", Toast.LENGTH_LONG).show();
                }
            }, 30000);
        } else {
            showDefaultLocation();
        }
    }

    private void moveCameraToLocation(Point point) {
        if (!isValidLocation(point)) {
            showDefaultLocation();
            return;
        }
        runOnUiThread(() -> {
            try {
                mapView.getMap().move(
                        new CameraPosition(
                                point,
                                14.0f,
                                0.0f,
                                0.0f
                        ),
                        new Animation(Animation.Type.SMOOTH, 1f),
                        null
                );
            } catch (Exception e) {
                Log.e("MapDebug", "Error moving camera", e);
            }
        });
    }

    private boolean isValidLocation(Point point) {
        return point.getLatitude() != 0 && point.getLongitude() != 0 &&
                point.getLatitude() >= -90 && point.getLatitude() <= 90 &&
                point.getLongitude() >= -180 && point.getLongitude() <= 180;
    }

    private void showDefaultLocation() {
        runOnUiThread(() -> {
            Toast.makeText(MapActivity.this,
                    "Не удалось определить местоположение. Используется центр Перми",
                    Toast.LENGTH_LONG).show();
            mapView.getMap().move(
                    new CameraPosition(
                            new Point(55.751244, 37.618423),
                            12.0f,
                            0.0f,
                            0.0f
                    ),
                    new Animation(Animation.Type.SMOOTH, 1f),
                    null
            );
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkLocationSettings();
            } else {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    Toast.makeText(this, "Разрешение на геолокацию отклонено. Включите его в настройках приложения.", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Разрешение на геолокацию отклонено", Toast.LENGTH_LONG).show();
                    showDefaultLocation();
                }
            }
        }
    }

    private void loadStoresAndSetupMap() {
        new LoadStoresTask().execute();
    }

    private class LoadStoresTask extends AsyncTask<Void, Void, List<Stores>> {
        @Override
        protected List<Stores> doInBackground(Void... voids) {
            try {
                String url = "https://mgxymxiehfsptuubuqfv.supabase.co/rest/v1/stores?select=*";
                Document doc = Jsoup.connect(url)
                        .header("Authorization", UserContext.TOKEN())
                        .header("apikey", UserContext.SECRET())
                        .ignoreContentType(true)
                        .get();
                JSONArray jsonArray = new JSONArray(doc.body().text());
                List<Stores> stores = new ArrayList<>();
                for (int i = 0; i < jsonArray.length(); i++) {
                    Stores store = new Stores(
                            jsonArray.getJSONObject(i).getInt("id"),
                            jsonArray.getJSONObject(i).getString("name"),
                            jsonArray.getJSONObject(i).getString("address")
                    );
                    stores.add(store);
                }
                return stores;
            } catch (Exception e) {
                Log.e("MapActivity", "Error loading stores", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<Stores> result) {
            if (result == null) {
                Toast.makeText(MapActivity.this,
                        "Ошибка загрузки магазинов",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            stores = result;
            for (Stores store : stores) {
                geocodeAndAddPlacemark(store);
            }
        }
    }

    private void setupBottomSheet() {
        bottomSheetDialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        bottomSheetView = getLayoutInflater().inflate(R.layout.store_info_bottom_sheet, null);
        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.getWindow().setDimAmount(0.6f);
        Button selectButton = bottomSheetView.findViewById(R.id.select_store_button);
        selectButton.setOnClickListener(v -> {
            if (selectedStore != null) {
                Log.d("MapActivity", "Selected store: ID=" + selectedStore.id + ", Address=" + selectedStore.address);
                Intent resultIntent = new Intent();
                resultIntent.putExtra("store_address", selectedStore.address);
                resultIntent.putExtra("store_id", selectedStore.id);
                PreferencesHelper.saveSelectedStore(MapActivity.this, selectedStore.address, selectedStore.id);
                setResult(RESULT_OK, resultIntent);
                bottomSheetDialog.dismiss();
                finish();
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            } else {
                Log.e("MapActivity", "Selected store is null");
                Toast.makeText(MapActivity.this, "Ошибка: магазин не выбран", Toast.LENGTH_SHORT).show();
            }
        });
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

    private void showStoreInfo(Stores store) {
        selectedStore = store;
        TextView storeName = bottomSheetView.findViewById(R.id.store_name);
        TextView storeAddress = bottomSheetView.findViewById(R.id.store_address);
        storeName.setText(store.name);
        storeAddress.setText(store.address);
        if (!bottomSheetDialog.isShowing()) {
            bottomSheetDialog.show();
        }
    }

    private void geocodeAndAddPlacemark(Stores store) {
        geocoderApi.geocode(GEOCODER_API_KEY, store.address, "json")
                .enqueue(new Callback<GeocoderResponse>() {
                    @Override
                    public void onResponse(Call<GeocoderResponse> call, Response<GeocoderResponse> response) {
                        if (response.isSuccessful() && response.body() != null &&
                                !response.body().response.GeoObjectCollection.featureMember.isEmpty()) {
                            GeocoderResponse.FeatureMember member = response.body().response.GeoObjectCollection.featureMember.get(0);
                            String[] coords = member.GeoObject.Point.pos.split(" ");
                            Point point = new Point(Double.parseDouble(coords[1]), Double.parseDouble(coords[0]));
                            runOnUiThread(() -> {
                                com.yandex.mapkit.map.MapObject placemark = mapView.getMap().getMapObjects().addPlacemark(
                                        point,
                                        ImageProvider.fromResource(MapActivity.this, R.drawable.map_point_activity),
                                        new IconStyle()
                                                .setAnchor(new PointF(0.5f, 1.0f))
                                                .setZIndex(1.0f)
                                );
                                placemark.addTapListener((mapObject, point1) -> {
                                    showStoreInfo(store);
                                    return true;
                                });
                            });
                        }
                    }

                    @Override
                    public void onFailure(Call<GeocoderResponse> call, Throwable t) {
                        Log.e("MapActivity", "Geocoding failed for: " + store.address, t);
                    }
                });
    }

    public void onBack(View view) {
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        mapView.onStop();
        MapKitFactory.getInstance().onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        MapKitFactory.getInstance().onStart();
        mapView.onStart();
    }
}
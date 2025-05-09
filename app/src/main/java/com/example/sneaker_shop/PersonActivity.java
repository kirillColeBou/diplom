package com.example.sneaker_shop;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;

import de.hdodenhof.circleimageview.CircleImageView;

public class PersonActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CAMERA_REQUEST = 2;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private EditText loginEditText, emailEditText, phoneEditText, addressEditText;
    private LinearLayout editButton;
    private View saveButton;
    private TextView profileTitle, editPhoto;
    private CircleImageView profileImageView;
    private long currentUserId;
    private boolean isEditMode = false;
    private String currentImageBase64;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.person_info_activity);
        loginEditText = findViewById(R.id.login);
        emailEditText = findViewById(R.id.email);
        phoneEditText = findViewById(R.id.phone_number);
        addressEditText = findViewById(R.id.address);
        editButton = findViewById(R.id.editButton);
        saveButton = findViewById(R.id.saveButton);
        profileTitle = findViewById(R.id.profileTitle);
        editPhoto = findViewById(R.id.editPhoto);
        profileImageView = findViewById(R.id.profile_image);
        currentUserId = AuthUtils.getCurrentUserId(this);
        if (currentUserId != -1L) {
            loadUserData(currentUserId);
        } else {
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadUserData(Long userId) {
        new LoadUserDataTask(userId).execute();
    }

    private class LoadUserDataTask extends AsyncTask<Void, Void, JSONArray> {
        private final Long userId;
        private String error;

        LoadUserDataTask(Long userId) {
            this.userId = userId;
        }

        @Override
        protected JSONArray doInBackground(Void... voids) {
            try {
                String url = UserContext.URL + "?user_uid=eq." + userId;
                Document doc = Jsoup.connect(url)
                        .header("Authorization", UserContext.TOKEN)
                        .header("apikey", UserContext.SECRET)
                        .ignoreContentType(true)
                        .get();
                return new JSONArray(doc.body().text());
            } catch (Exception e) {
                error = e.getMessage();
                return null;
            }
        }

        @Override
        protected void onPostExecute(JSONArray result) {
            if (result != null && result.length() > 0) {
                try {
                    String login = result.getJSONObject(0).isNull("login") ? "" : result.getJSONObject(0).optString("login", "");
                    String email = result.getJSONObject(0).isNull("email") ? "" : result.getJSONObject(0).optString("email", "");
                    String phone = result.getJSONObject(0).isNull("phone_number") ? "" : result.getJSONObject(0).optString("phone_number", "");
                    String address = result.getJSONObject(0).isNull("address") ? "" : result.getJSONObject(0).optString("address", "");
                    String imageBase64 = result.getJSONObject(0).isNull("image") ? null :
                            result.getJSONObject(0).optString("image", null);
                    if (imageBase64 != null && !imageBase64.isEmpty()) {
                        byte[] decodedString = Base64.decode(imageBase64, Base64.DEFAULT);
                        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        profileImageView.setImageBitmap(decodedByte);
                    }
                    setFieldWithHint(loginEditText, login, "Введите логин");
                    setFieldWithHint(emailEditText, email, "Введите email");
                    setFieldWithHint(phoneEditText, phone, "Введите телефон");
                    setFieldWithHint(addressEditText, address, "Введите адрес");
                } catch (Exception e) {
                    Toast.makeText(PersonActivity.this, "Ошибка обработки данных", Toast.LENGTH_SHORT).show();
                }
            } else if (error != null) {
                Toast.makeText(PersonActivity.this, "Ошибка загрузки данных: " + error, Toast.LENGTH_SHORT).show();
            }
        }

        private void setFieldWithHint(EditText editText, String value, String hint) {
            if (value == null || value.isEmpty() || value.equals("null")) {
                editText.setText("");
                editText.setHint(hint);
            } else {
                editText.setText(value);
            }
        }
    }

    public void onRedact(View view) {
        isEditMode = true;
        loginEditText.setEnabled(true);
        emailEditText.setEnabled(true);
        phoneEditText.setEnabled(true);
        addressEditText.setEnabled(true);
        editButton.setVisibility(View.GONE);
        saveButton.setVisibility(View.VISIBLE);
        editPhoto.setVisibility(View.VISIBLE);
        profileTitle.setPadding(0, 0, 0, 0);
    }

    public void onSave(View view) {
        if (loginEditText.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Поле логин не заполнено", Toast.LENGTH_SHORT).show();
            return;
        }
        if (emailEditText.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Поле эл. почты не заполнено", Toast.LENGTH_SHORT).show();
            return;
        }
        new SaveUserDataTask().execute();
    }

    private class SaveUserDataTask extends AsyncTask<Void, Void, Boolean> {
        private String error;

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                String url = UserContext.URL + "?user_uid=eq." + currentUserId;
                String data = String.format(
                        "{\"login\":\"%s\",\"email\":\"%s\",\"phone_number\":\"%s\",\"address\":\"%s\"}",
                        loginEditText.getText().toString(),
                        emailEditText.getText().toString(),
                        phoneEditText.getText().toString(),
                        addressEditText.getText().toString()
                );
                org.jsoup.Connection.Response response = Jsoup.connect(url)
                        .header("Authorization", UserContext.TOKEN)
                        .header("apikey", UserContext.SECRET)
                        .header("Content-Type", "application/json")
                        .header("Prefer", "return=minimal")
                        .requestBody(data)
                        .method(org.jsoup.Connection.Method.PATCH)
                        .ignoreContentType(true)
                        .execute();
                return response.statusCode() == 204 || response.statusCode() == 200;
            } catch (Exception e) {
                error = e.getMessage();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                isEditMode = false;
                loginEditText.setEnabled(false);
                emailEditText.setEnabled(false);
                phoneEditText.setEnabled(false);
                addressEditText.setEnabled(false);
                editButton.setVisibility(View.VISIBLE);
                saveButton.setVisibility(View.GONE);
                editPhoto.setVisibility(View.GONE);
                profileTitle.setPadding(40, 0, 0, 0);
                Toast.makeText(PersonActivity.this, "Данные сохранены", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(PersonActivity.this, "Ошибка сохранения: " + error, Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void onEditPhoto(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        PERMISSION_REQUEST_CODE);
            } else {
                showImagePickerDialog();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
            } else {
                showImagePickerDialog();
            }
        }
    }

    private void showImagePickerDialog() {
        try {
            Intent galleryIntent = new Intent(Intent.ACTION_PICK);
            galleryIntent.setType("image/*");
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            Intent chooserIntent = Intent.createChooser(galleryIntent, "Выберите источник");
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{cameraIntent});
            startActivityForResult(chooserIntent, PICK_IMAGE_REQUEST);
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("ImagePicker", "Error: ", e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }
        try {
            Bitmap bitmap = null;
            if (requestCode == PICK_IMAGE_REQUEST) {
                if (data != null && data.getData() != null) {
                    Uri selectedImageUri = data.getData();
                    InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
                    bitmap = BitmapFactory.decodeStream(inputStream);
                    if (inputStream != null) {
                        inputStream.close();
                    }
                } else if (data != null && data.getExtras() != null) {
                    bitmap = (Bitmap) data.getExtras().get("data");
                }
            }

            if (bitmap != null) {
                bitmap = getResizedBitmap(bitmap, 800);
                profileImageView.setImageBitmap(bitmap);
                currentImageBase64 = bitmapToBase64(bitmap);
                if (currentImageBase64 != null) {
                    saveImageToDatabase();
                }
            } else {
                Toast.makeText(this, "Не удалось загрузить изображение", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка обработки изображения: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("ImageError", "Error processing image", e);
        }
    }

    private void saveImageToDatabase() {
        if (currentImageBase64 == null || currentImageBase64.isEmpty()) {
            Toast.makeText(this, "Нет изображения для сохранения", Toast.LENGTH_SHORT).show();
            return;
        }
        String cleanedBase64 = currentImageBase64.replace("\n", "").trim();
        new AsyncTask<Void, Void, Boolean>() {
            private String error;

            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    String url = UserContext.URL + "?user_uid=eq." + currentUserId;
                    String data = String.format("{\"image\":\"%s\"}", cleanedBase64);
                    Log.d("SaveImage", "Sending data: " + data);
                    org.jsoup.Connection.Response response = Jsoup.connect(url)
                            .header("Authorization", UserContext.TOKEN)
                            .header("apikey", UserContext.SECRET)
                            .header("Content-Type", "application/json")
                            .header("Prefer", "return=minimal")
                            .requestBody(data)
                            .method(org.jsoup.Connection.Method.PATCH)
                            .ignoreContentType(true)
                            .execute();
                    Log.d("SaveImage", "Response: " + response.statusCode());
                    return response.statusCode() == 204 || response.statusCode() == 200;
                } catch (Exception e) {
                    error = e.getMessage();
                    Log.e("SaveImage", "Error: " + error, e);
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (success) {
                    Toast.makeText(PersonActivity.this, "Фото профиля сохранено", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(PersonActivity.this, "Ошибка сохранения фото: " + error, Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();
    }

    private Bitmap getResizedBitmap(Bitmap image, int maxWidth) {
        int width = image.getWidth();
        int height = image.getHeight();
        if (width <= maxWidth) {
            return image;
        }
        float ratio = (float) width / (float) height;
        int newHeight = (int) (maxWidth / ratio);
        return Bitmap.createScaledBitmap(image, maxWidth, newHeight, true);
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private class SaveImageTask extends AsyncTask<Void, Void, Boolean> {
        private String error;

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                String url = UserContext.URL + "?user_uid=eq." + currentUserId;
                String data = String.format("{\"image\":\"%s\"}", currentImageBase64);
                org.jsoup.Connection.Response response = Jsoup.connect(url)
                        .header("Authorization", UserContext.TOKEN)
                        .header("apikey", UserContext.SECRET)
                        .header("Content-Type", "application/json")
                        .header("Prefer", "return=minimal")
                        .requestBody(data)
                        .method(org.jsoup.Connection.Method.PATCH)
                        .ignoreContentType(true)
                        .execute();

                return response.statusCode() == 204 || response.statusCode() == 200;
            } catch (Exception e) {
                error = e.getMessage();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                Toast.makeText(PersonActivity.this, "Фото профиля сохранено", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(PersonActivity.this, "Ошибка сохранения фото: " + error, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showImagePickerDialog();
            } else {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    showPermissionSettingsDialog();
                } else {
                    Toast.makeText(this,
                            "Доступ к хранилищу отклонен",
                            Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void showPermissionSettingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Требуется доступ к хранилищу")
                .setMessage("Вы запретили запрос разрешения. Пожалуйста, предоставьте доступ в настройках")
                .setPositiveButton("Настройки", (dialog, which) -> openAppSettings())
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    public void onHome(View view){
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(0, 0);
        finish();
    }

    public void onMenu(View view){
        startActivity(new Intent(this, MenuActivity.class));
        overridePendingTransition(0, 0);
        finish();
    }

    public void onFavorite(View view){
        startActivity(new Intent(this, FavoriteActivity.class));
        overridePendingTransition(0, 0);
        finish();
    }
}
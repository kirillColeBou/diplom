package com.example.sneaker_shop;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.net.URLEncoder;
import de.hdodenhof.circleimageview.CircleImageView;

public class MenuActivity extends AppCompatActivity {
    private CircleImageView personImageView;
    private TextView loginTextView;
    private long currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu_activity);
        personImageView = findViewById(R.id.person_image);
        loginTextView = findViewById(R.id.login);
        currentUserId = AuthUtils.getCurrentUserId(this);
        if (currentUserId != -1L) {
            loadUserData(String.valueOf(currentUserId));
        } else {
            setDefaultData();
        }
    }

    private void setDefaultData() {
        loginTextView.setText("Гость");
        personImageView.setImageResource(R.drawable.image_person);
    }

    private void loadUserData(String userId) {
        new LoadUserDataTask().execute(userId);
    }

    private class LoadUserDataTask extends AsyncTask<String, Void, JSONArray> {
        @Override
        protected JSONArray doInBackground(String... params) {
            String userId = params[0];
            try {
                String url = UserContext.URL + "?user_uid=eq." + URLEncoder.encode(userId, "UTF-8");
                Document doc = Jsoup.connect(url)
                        .header("Authorization", UserContext.TOKEN)
                        .header("apikey", UserContext.SECRET)
                        .ignoreContentType(true)
                        .get();
                return new JSONArray(doc.body().text());
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(JSONArray result) {
            if (result != null && result.length() > 0) {
                try {
                    String login = result.getJSONObject(0).optString("login", "");
                    loginTextView.setText(login.isEmpty() ? "Пользователь" : login);
                    String imageBase64 = result.getJSONObject(0).optString("image", null);
                    if (imageBase64 != null && !imageBase64.isEmpty() && !imageBase64.equals("null")) {
                        byte[] decodedString = Base64.decode(imageBase64, Base64.DEFAULT);
                        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        personImageView.setImageBitmap(decodedByte);
                    } else {
                        personImageView.setImageResource(R.drawable.image_person);
                    }
                } catch (Exception e) {
                    setDefaultData();
                }
            } else {
                setDefaultData();
            }
        }
    }

    public void onHome(View view){
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(0, 0);
        finish();
    }

    public void onFavorite(View view){
        startActivity(new Intent(this, FavoriteActivity.class));
        overridePendingTransition(0, 0);
        finish();
    }

    public void onPerson(View view){
        startActivity(new Intent(this, PersonActivity.class));
        overridePendingTransition(0, 0);
        finish();
    }

    public void onCart(View view) {
        startActivity(new Intent(this, CartActivity.class));
        overridePendingTransition(0, 0);
        finish();
    }

    public void onLogout(View view) {
        AuthUtils.logout(this);
        PreferencesHelper.clearSelectedStore(this);
        Intent intent = new Intent(this, AuthorizationActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        finish();
    }

    public void onDelete(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.RoundedDialog);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.alert_dialog, null);
        builder.setView(dialogView);
        TextView title = dialogView.findViewById(R.id.title);
        TextView message = dialogView.findViewById(R.id.message);
        Button negativeButton = dialogView.findViewById(R.id.negative_button);
        Button positiveButton = dialogView.findViewById(R.id.positive_button);
        title.setText("Подтверждение удаления");
        message.setText("Вы уверены, что хотите удалить всю информацию о своем аккаунте?");
        AlertDialog dialog = builder.create();
        negativeButton.setOnClickListener(v -> dialog.dismiss());
        positiveButton.setOnClickListener(v -> {
            dialog.dismiss();
            deleteUserAccount();
        });
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(window.getAttributes());
            DisplayMetrics displayMetrics = new DisplayMetrics();
            window.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int screenWidth = displayMetrics.widthPixels;
            int horizontalMargin = (int) (20 * displayMetrics.density);
            layoutParams.width = screenWidth - 2 * horizontalMargin;
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(layoutParams);
            window.setBackgroundDrawableResource(R.drawable.background_alert_dialog);
        }
    }

    private void deleteUserAccount() {
        UserContext.deleteUser(currentUserId, new UserContext.DeleteUserCallback() {
            @Override
            public void onSuccess() {
                AuthUtils.logout(MenuActivity.this);
                Intent intent = new Intent(MenuActivity.this, AuthorizationActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                Toast.makeText(MenuActivity.this, "Аккаунт успешно удален", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(MenuActivity.this,
                        "Ошибка при удалении аккаунта: " + error,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
package com.example.sneaker_shop;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.TextView;
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
                String url = UserContext.URL + "?id=eq." + URLEncoder.encode(userId, "UTF-8");
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

    public void onLogout(View view) {
        AuthUtils.logout(this);
        Intent intent = new Intent(this, AuthorizationActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        finish();
    }
}
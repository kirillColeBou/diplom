package com.example.sneaker_shop;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.URLEncoder;

public class ForgotPasswordActivity extends AppCompatActivity {
    private EditText newPasswordEditText;
    private EditText confirmPasswordEditText;
    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.forgot_password_activity);
        email = getIntent().getStringExtra("email");
        newPasswordEditText = findViewById(R.id.new_password_edittext);
        confirmPasswordEditText = findViewById(R.id.confirm_password_edittext);
        findViewById(R.id.reset_password_button).setOnClickListener(v -> resetPassword());
    }

    private void resetPassword() {
        String newPassword = newPasswordEditText.getText().toString();
        String confirmPassword = confirmPasswordEditText.getText().toString();
        if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_SHORT).show();
            return;
        }
        new UpdatePasswordTask().execute(email, md5(newPassword));
    }

    private class UpdatePasswordTask extends AsyncTask<String, Void, Boolean> {
        private String error;

        @Override
        protected Boolean doInBackground(String... params) {
            try {
                String email = params[0];
                String newPasswordHash = params[1];
                String url = RegisterContext.URL + "?email=eq." + URLEncoder.encode(email, "UTF-8");
                String jsonBody = "{\"password\":\"" + newPasswordHash + "\"}";
                Document doc = Jsoup.connect(url)
                        .header("Authorization", UserContext.TOKEN())
                        .header("apikey", UserContext.SECRET())
                        .header("Content-Type", "application/json")
                        .header("Prefer", "return=minimal")
                        .requestBody(jsonBody)
                        .method(org.jsoup.Connection.Method.PATCH)
                        .ignoreContentType(true)
                        .execute()
                        .parse();
                return true;
            } catch (Exception e) {
                error = e.getMessage();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                Toast.makeText(ForgotPasswordActivity.this,
                        "Пароль успешно изменен", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(ForgotPasswordActivity.this, AuthorizationActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(ForgotPasswordActivity.this,
                        "Ошибка: " + error, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String md5(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : array) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 error", e);
        }
    }

    public void onBack(View view) {
        startActivity(new Intent(this, RestorePasswordActivity.class));
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        finish();
    }
}
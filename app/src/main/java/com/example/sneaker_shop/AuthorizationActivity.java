package com.example.sneaker_shop;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class AuthorizationActivity extends AppCompatActivity {
    private EditText logins;
    private EditText passwords;
    private ImageView eyeIcon;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.authorization_activity);
        logins = findViewById(R.id.logins);
        passwords = findViewById(R.id.password);
        eyeIcon = findViewById(R.id.eyeIcon);
        eyeIcon.setOnClickListener(v -> togglePasswordVisibility());
    }

    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            passwords.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            eyeIcon.setImageResource(R.drawable.eye);
        } else {
            passwords.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            eyeIcon.setImageResource(R.drawable.eye_open);
        }
        passwords.setSelection(passwords.getText().length());
        isPasswordVisible = !isPasswordVisible;
    }

    public void onAuthorization(View view) {
        String loginOrEmailOrPhone = logins.getText().toString().trim();
        String password = passwords.getText().toString().trim();
        if (loginOrEmailOrPhone.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }
        String hashedPassword = md5(password);
        UserContext.checkUserCredentials(loginOrEmailOrPhone, hashedPassword, new UserContext.Callback() {
            @Override
            public void onSuccess(boolean userExists) {
                runOnUiThread(() -> {
                    if (userExists) {
                        UserContext.getUserId(loginOrEmailOrPhone, new UserContext.UserIdCallback() {
                            @Override
                            public void onSuccess(String userId) {
                                AuthUtils.saveUserCredentials(AuthorizationActivity.this,
                                        loginOrEmailOrPhone, hashedPassword, userId);
                                startActivity(new Intent(AuthorizationActivity.this, MainActivity.class));
                                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                                finish();
                            }

                            @Override
                            public void onError(String error) {
                                Toast.makeText(AuthorizationActivity.this,
                                        "Ошибка получения данных пользователя",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        Toast.makeText(AuthorizationActivity.this,
                                "Неверные учетные данные", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() ->
                        Toast.makeText(AuthorizationActivity.this,
                                "Ошибка соединения: " + error, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private String md5(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }

    public void onRestorePassword(View view){
        startActivity(new Intent(AuthorizationActivity.this, RestorePasswordActivity.class));
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    public void onRegister(View view){
        startActivity(new Intent(AuthorizationActivity.this, RegisterActivity.class));
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }
}
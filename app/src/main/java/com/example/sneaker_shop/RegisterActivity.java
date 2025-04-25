package com.example.sneaker_shop;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {
    EditText emails, phone_numbers, passwords;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_activity);
        emails = findViewById(R.id.email);
        phone_numbers = findViewById(R.id.phone_number);
        passwords = findViewById(R.id.password);
    }

    public void onRegistration(View view) {
        String email = emails.getText().toString().trim();
        String phone_number = phone_numbers.getText().toString().trim();
        String password = passwords.getText().toString().trim();

        if (validateInputs(email, phone_number, password)) {
            checkUserAndProceed(email, phone_number, password);
        }
    }

    private boolean validateInputs(String email, String phone, String password) {
        if (email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emails.setError("Введите корректный email");
            emails.requestFocus();
            return false;
        }

        String cleanedPhone = phone.replaceAll("[^0-9]", "");
        if (cleanedPhone.length() < 10) {
            phone_numbers.setError("Введите корректный номер телефона");
            phone_numbers.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            passwords.setError("Пароль должен содержать минимум 6 символов");
            passwords.requestFocus();
            return false;
        }

        return true;
    }

    private void checkUserAndProceed(String email, String phone, String password) {
        RegisterContext.checkUserExists(email, phone, new RegisterContext.Callback() {
            @Override
            public void onSuccess(boolean isAvailable) {
                if (isAvailable) {
                    proceedToVerification(email, phone, password);
                } else {
                    showUserExistsError();
                }
            }

            @Override
            public void onError(String error) {
                showRegistrationError(error);
            }
        });
    }

    private void proceedToVerification(String email, String phone, String password) {
        Intent intent = new Intent(RegisterActivity.this, VerificationActivity.class);
        intent.putExtra("email", email);
        intent.putExtra("phone", phone);
        intent.putExtra("password", password);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        finish();
    }

    private void showUserExistsError() {
        runOnUiThread(() ->
                Toast.makeText(RegisterActivity.this,
                        "Пользователь с такими данными уже существует!",
                        Toast.LENGTH_SHORT).show());
    }

    private void showRegistrationError(String error) {
        runOnUiThread(() ->
                Toast.makeText(RegisterActivity.this,
                        "Ошибка: " + error, Toast.LENGTH_SHORT).show());
    }

    public void onBack(View view) {
        startActivity(new Intent(RegisterActivity.this, AuthorizationActivity.class));
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }
}
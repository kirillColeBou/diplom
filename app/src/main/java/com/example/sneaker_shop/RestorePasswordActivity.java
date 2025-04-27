package com.example.sneaker_shop;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RestorePasswordActivity extends AppCompatActivity {
    private EditText emailEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.restore_password_activity);
        emailEditText = findViewById(R.id.email_edittext);
        findViewById(R.id.submit_button).setOnClickListener(v -> sendVerificationCode());
    }

    private void sendVerificationCode() {
        String email = emailEditText.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(this, "Введите email", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, VerificationActivity.class);
        intent.putExtra("email", email);
        intent.putExtra("mode", "password_reset");
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    public void onBack(View view) {
        startActivity(new Intent(this, AuthorizationActivity.class));
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        finish();
    }
}
package com.example.sneaker_shop;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class RestorePasswordActivity extends AppCompatActivity {
    private EditText emailEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.restore_password_activity);
        emailEditText = findViewById(R.id.email_edittext);
        findViewById(R.id.submit_button).setOnClickListener(v -> checkUserExists());
        View rootLayout = findViewById(R.id.main5);
        rootLayout.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                hideKeyboard();
            }
            return false;
        });
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View view = getCurrentFocus();
        if (view != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void checkUserExists() {
        String email = emailEditText.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(this, "Введите email", Toast.LENGTH_SHORT).show();
            return;
        }
        new CheckUserTask().execute(email);
    }

    @SuppressLint("StaticFieldLeak")
    private class CheckUserTask extends AsyncTask<String, Void, Boolean> {
        private String errorMessage;

        @Override
        protected Boolean doInBackground(String... emails) {
            try {
                String email = emails[0];
                String url = RegisterContext.URL + "?email=eq." + email + "&select=user_uid";
                Document doc = Jsoup.connect(url)
                        .header("Authorization", UserContext.TOKEN())
                        .header("apikey", UserContext.SECRET())
                        .ignoreContentType(true)
                        .get();
                return doc.body().text().contains("user_uid");
            } catch (Exception e) {
                errorMessage = e.getMessage();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean userExists) {
            if (userExists) {
                String email = emailEditText.getText().toString().trim();
                Intent intent = new Intent(RestorePasswordActivity.this, VerificationActivity.class);
                intent.putExtra("email", email);
                intent.putExtra("mode", "password_reset");
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            } else {
                Toast.makeText(RestorePasswordActivity.this,
                        "Пользователь не найден", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void onBack(View view) {
        startActivity(new Intent(this, AuthorizationActivity.class));
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        finish();
    }
}
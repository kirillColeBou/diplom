package com.example.sneaker_shop;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (AuthUtils .isUserLoggedIn(this)) {
            String[] credentials = AuthUtils.getSavedCredentials(this);
            if (credentials != null) {
                UserContext.checkUserCredentials(credentials[0], credentials[1], new UserContext.Callback() {
                    @Override
                    public void onSuccess(boolean userExists) {
                        runOnUiThread(() -> {
                            if (userExists) {
                                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                                finish();
                            } else {
                                AuthUtils.logout(SplashActivity.this);
                                goToAuthorization();
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            goToAuthorization();
                        });
                    }
                });
                return;
            }
        }
        goToAuthorization();
    }

    private void goToAuthorization() {
        startActivity(new Intent(this, AuthorizationActivity.class));
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        finish();
    }
}

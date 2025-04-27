package com.example.sneaker_shop;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.view.GestureDetectorCompat;
import android.view.GestureDetector;
import android.view.MotionEvent;

public class OnBoardActivity extends AppCompatActivity {

    private int currentPage = 0;
    private final int[] pages = {R.id.page1, R.id.page2, R.id.page3};
    private GestureDetectorCompat gestureDetector;
    private final long ANIMATION_DELAY = 250;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.onboard_activity);
        gestureDetector = new GestureDetectorCompat(this, new GestureListener());
        AppCompatButton btnStart = findViewById(R.id.btn_start);
        AppCompatButton btnJoinCommunity = findViewById(R.id.btn_join_community);
        AppCompatButton btnJoinNow = findViewById(R.id.btn_join_now);
        btnStart.setOnClickListener(v -> showNextPage());
        btnJoinCommunity.setOnClickListener(v -> showNextPage());
        btnJoinNow.setOnClickListener(v -> {
            startActivity(new Intent(OnBoardActivity.this, MainActivity.class));
            finish();
        });
        resetAndShowPage(0);
    }

    private void hideAllElements() {
        findViewById(R.id.welcome_text).setAlpha(0);
        findViewById(R.id.image1).setAlpha(0);
        findViewById(R.id.btn_start).setAlpha(0);
        findViewById(R.id.image2).setAlpha(0);
        findViewById(R.id.title2).setAlpha(0);
        findViewById(R.id.subtitle2).setAlpha(0);
        findViewById(R.id.btn_join_community).setAlpha(0);
        findViewById(R.id.image3).setAlpha(0);
        findViewById(R.id.title3).setAlpha(0);
        findViewById(R.id.subtitle3).setAlpha(0);
        findViewById(R.id.btn_join_now).setAlpha(0);
    }

    private void resetAndShowPage(int pageIndex) {
        hideAllElements();
        for (int i = 0; i < pages.length; i++) {
            LinearLayout page = findViewById(pages[i]);
            if (page != null) {
                page.setVisibility(i == pageIndex ? View.VISIBLE : View.GONE);
            }
        }
        showPageAnimations(pageIndex);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            boolean result = false;
            try {
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            showPreviousPage();
                        } else {
                            showNextPage();
                        }
                        result = true;
                    }
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            return result;
        }
    }

    private void showNextPage() {
        if (currentPage < pages.length - 1) {
            currentPage++;
            switchPage();
        } else {
            startActivity(new Intent(this, MainActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            finish();
        }
    }

    private void showPreviousPage() {
        if (currentPage > 0) {
            currentPage--;
            switchPage();
        }
    }

    private void switchPage() {
        resetAndShowPage(currentPage);
        for (int i = 0; i < pages.length; i++) {
            LinearLayout page = findViewById(pages[i]);
            if (page != null) {
                if (i == currentPage) {
                    page.setVisibility(View.VISIBLE);
                    page.startAnimation(AnimationUtils.loadAnimation(this,
                            currentPage > i ? R.anim.slide_out_left : R.anim.slide_in_right));
                } else {
                    page.setVisibility(View.GONE);
                }
            }
        }
        showPageAnimations(currentPage);
    }

    private void showPageAnimations(int pageIndex) {
        switch (pageIndex) {
            case 0:
                animateFirstPage();
                break;
            case 1:
                animateSecondPage();
                break;
            case 2:
                animateThirdPage();
                break;
        }
    }

    private void animateFirstPage() {
        View welcomeText = findViewById(R.id.welcome_text);
        View image1 = findViewById(R.id.image1);
        View btnStart = findViewById(R.id.btn_start);
        welcomeText.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        welcomeText.postDelayed(() -> {
            welcomeText.setAlpha(1);
            image1.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_up));
            image1.postDelayed(() -> {
                image1.setAlpha(1);
                btnStart.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_up));
                btnStart.postDelayed(() -> btnStart.setAlpha(1), ANIMATION_DELAY);
            }, ANIMATION_DELAY);
        }, ANIMATION_DELAY);
    }

    private void animateSecondPage() {
        View image2 = findViewById(R.id.image2);
        View title2 = findViewById(R.id.title2);
        View subtitle2 = findViewById(R.id.subtitle2);
        View btnJoinCommunity = findViewById(R.id.btn_join_community);
        image2.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        image2.postDelayed(() -> {
            image2.setAlpha(1);
            title2.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_up));
            title2.postDelayed(() -> {
                title2.setAlpha(1);
                subtitle2.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
                subtitle2.postDelayed(() -> {
                    subtitle2.setAlpha(1);
                    btnJoinCommunity.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_up));
                    btnJoinCommunity.postDelayed(() -> btnJoinCommunity.setAlpha(1), ANIMATION_DELAY);
                }, ANIMATION_DELAY);
            }, ANIMATION_DELAY);
        }, ANIMATION_DELAY);
    }

    private void animateThirdPage() {
        View image3 = findViewById(R.id.image3);
        View title3 = findViewById(R.id.title3);
        View subtitle3 = findViewById(R.id.subtitle3);
        View btnJoinNow = findViewById(R.id.btn_join_now);
        image3.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        image3.postDelayed(() -> {
            image3.setAlpha(1);
            title3.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_up));
            title3.postDelayed(() -> {
                title3.setAlpha(1);
                subtitle3.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
                subtitle3.postDelayed(() -> {
                    subtitle3.setAlpha(1);
                    btnJoinNow.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_up));
                    btnJoinNow.postDelayed(() -> btnJoinNow.setAlpha(1), ANIMATION_DELAY);
                }, ANIMATION_DELAY);
            }, ANIMATION_DELAY);
        }, ANIMATION_DELAY);
    }
}

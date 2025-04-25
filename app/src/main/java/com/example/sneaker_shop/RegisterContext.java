package com.example.sneaker_shop;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.URLEncoder;

public class RegisterContext {
    public static final String URL = "https://mgxymxiehfsptuubuqfv.supabase.co/rest/v1/users";
    public static final String TOKEN = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im1neHlteGllaGZzcHR1dWJ1cWZ2Iiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc0NTIyNzY0NSwiZXhwIjoyMDYwODAzNjQ1fQ.LNqLc1o8I8eZUxYuFXknXZZhzN5kRh0eggmg5tItiM0";
    public static final String SECRET = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im1neHlteGllaGZzcHR1dWJ1cWZ2Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDUyMjc2NDUsImV4cCI6MjA2MDgwMzY0NX0.QXcy5Dpd4_b58-xfpvPAIgm9U8Pj6w62RW6p7NDUKyQ";
    private static final String CHECK_USER_URL = URL + "?select=email,phone_number&or=(email.eq.%s,phone_number.eq.%s)";

    public interface Callback {
        void onSuccess(boolean isAvailable);
        void onError(String error);
    }

    public static void checkUserExists(String email, String phone, Callback callback) {
        new CheckUserTask(email, phone, callback).execute();
    }

    private static class CheckUserTask extends AsyncTask<Void, Void, Boolean> {
        private final String email;
        private final String phone;
        private final Callback callback;
        private String error;

        CheckUserTask(String email, String phone, Callback callback) {
            this.email = email;
            this.phone = phone;
            this.callback = callback;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                String checkUrl = String.format(CHECK_USER_URL,
                        URLEncoder.encode(email, "UTF-8"),
                        URLEncoder.encode(phone, "UTF-8"));
                Document doc = Jsoup.connect(checkUrl)
                        .header("Authorization", TOKEN)
                        .header("apikey", SECRET)
                        .ignoreContentType(true)
                        .get();
                JSONArray users = new JSONArray(doc.body().text());
                return users.length() == 0;
            } catch (Exception e) {
                error = e.getMessage();
                Log.e("Registration", "Error: " + error);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean isAvailable) {
            if (error != null) {
                callback.onError(error);
            } else {
                callback.onSuccess(isAvailable);
            }
        }
    }
}
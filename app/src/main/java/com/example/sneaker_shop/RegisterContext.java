package com.example.sneaker_shop;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.URLEncoder;

public class RegisterContext {
    public static final String URL = "https://mgxymxiehfsptuubuqfv.supabase.co/rest/v1/users";
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
                        .header("Authorization", UserContext.TOKEN())
                        .header("apikey", UserContext.SECRET())
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
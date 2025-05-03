package com.example.sneaker_shop;


import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.URLEncoder;

public class UserContext {
    public static final String URL = "https://mgxymxiehfsptuubuqfv.supabase.co/rest/v1/users";
    public static final String TOKEN = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im1neHlteGllaGZzcHR1dWJ1cWZ2Iiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc0NTIyNzY0NSwiZXhwIjoyMDYwODAzNjQ1fQ.LNqLc1o8I8eZUxYuFXknXZZhzN5kRh0eggmg5tItiM0";
    public static final String SECRET = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im1neHlteGllaGZzcHR1dWJ1cWZ2Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDUyMjc2NDUsImV4cCI6MjA2MDgwMzY0NX0.QXcy5Dpd4_b58-xfpvPAIgm9U8Pj6w62RW6p7NDUKyQ";

    public interface Callback {
        void onSuccess(boolean userExists);
        void onError(String error);
    }

    public static void checkUserCredentials(String loginOrEmailOrPhone, String password, Callback callback) {
        new CheckUserTask(loginOrEmailOrPhone, password, callback).execute();
    }

    private static class CheckUserTask extends AsyncTask<Void, Void, Boolean> {
        private final String loginOrEmailOrPhone;
        private final String password;
        private final Callback callback;
        private String error;

        CheckUserTask(String loginOrEmailOrPhone, String password, Callback callback) {
            this.loginOrEmailOrPhone = loginOrEmailOrPhone;
            this.password = password;
            this.callback = callback;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                String url = URL + "?or=" +
                        URLEncoder.encode("(login.eq." + loginOrEmailOrPhone +
                                ",email.eq." + loginOrEmailOrPhone +
                                ",phone_number.eq." + loginOrEmailOrPhone + ")", "UTF-8") +
                        "&password=eq." + URLEncoder.encode(password, "UTF-8");
                Document doc = Jsoup.connect(url)
                        .header("Authorization", TOKEN)
                        .header("apikey", SECRET)
                        .ignoreContentType(true)
                        .get();
                String response = doc.body().text();
                Log.d("Supabase", "Response: " + response);
                return new JSONArray(response).length() > 0;
            } catch (Exception e) {
                error = "Error: " + e.getMessage();
                Log.e("Supabase", "Request failed: " + error);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean userExists) {
            if (error != null) {
                callback.onError(error);
            } else {
                callback.onSuccess(userExists);
            }
        }
    }

    public static void getUserId(String loginOrEmailOrPhone, UserIdCallback callback) {
        new GetUserIdTask(loginOrEmailOrPhone, callback).execute();
    }

    public interface UserIdCallback {
        void onSuccess(String userId);
        void onError(String error);
    }

    private static class GetUserIdTask extends AsyncTask<Void, Void, String> {
        private final String loginOrEmailOrPhone;
        private final UserIdCallback callback;
        private String error;

        GetUserIdTask(String loginOrEmailOrPhone, UserIdCallback callback) {
            this.loginOrEmailOrPhone = loginOrEmailOrPhone;
            this.callback = callback;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                String url = URL + "?or=" +
                        URLEncoder.encode("(login.eq." + loginOrEmailOrPhone +
                                ",email.eq." + loginOrEmailOrPhone +
                                ",phone_number.eq." + loginOrEmailOrPhone + ")") +
                        "&select=id";

                Document doc = Jsoup.connect(url)
                        .header("Authorization", TOKEN)
                        .header("apikey", SECRET)
                        .ignoreContentType(true)
                        .get();

                JSONArray jsonArray = new JSONArray(doc.body().text());
                if (jsonArray.length() > 0) {
                    return jsonArray.getJSONObject(0).getString("id");
                }
                return null;
            } catch (Exception e) {
                error = "Error: " + e.getMessage();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String userId) {
            if (error != null) {
                callback.onError(error);
            } else if (userId != null) {
                callback.onSuccess(userId);
            } else {
                callback.onError("User not found");
            }
        }
    }
}

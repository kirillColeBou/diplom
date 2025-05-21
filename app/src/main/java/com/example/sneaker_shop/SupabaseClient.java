package com.example.sneaker_shop;

import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class SupabaseClient {
    protected static final String BASE_URL = "https://mgxymxiehfsptuubuqfv.supabase.co/rest/v1/";
    protected static final String TOKEN = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im1neHlteGllaGZzcHR1dWJ1cWZ2Iiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc0NTIyNzY0NSwiZXhwIjoyMDYwODAzNjQ1fQ.LNqLc1o8I8eZUxYuFXknXZZhzN5kRh0eggmg5tItiM0";
    protected static final String SECRET = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im1neHlteGllaGZzcHR1dWJ1cWZ2Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDUyMjc2NDUsImV4cCI6MjA2MDgwMzY0NX0.QXcy5Dpd4_b58-xfpvPAIgm9U8Pj6w62RW6p7NDUKyQ";

    protected static Document executeGet(String endpoint, String params) throws Exception {
        return Jsoup.connect(BASE_URL + endpoint + (params != null ? "?" + params : ""))
                .header("Authorization", TOKEN)
                .header("apikey", SECRET)
                .ignoreContentType(true)
                .get();
    }

    protected static Connection.Response executePost(String endpoint, JSONObject data) throws Exception {
        return Jsoup.connect(BASE_URL + endpoint)
                .header("Authorization", TOKEN)
                .header("apikey", SECRET)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=minimal")
                .requestBody(data.toString())
                .ignoreContentType(true)
                .method(Connection.Method.POST)
                .execute();
    }

    protected static Connection.Response executeDelete(String endpoint, String params) throws Exception {
        return Jsoup.connect(BASE_URL + endpoint + (params != null ? "?" + params : ""))
                .header("Authorization", TOKEN)
                .header("apikey", SECRET)
                .ignoreContentType(true)
                .method(Connection.Method.DELETE)
                .execute();
    }

    protected static Document executeRpc(String functionName, JSONObject params) throws Exception {
        return Jsoup.connect(BASE_URL + "rpc/" + functionName)
                .header("Authorization", TOKEN)
                .header("apikey", SECRET)
                .header("Content-Type", "application/json")
                .requestBody(params.toString())
                .ignoreContentType(true)
                .post();
    }
}
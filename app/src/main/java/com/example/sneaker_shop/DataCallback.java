package com.example.sneaker_shop;

public interface DataCallback<T> {
    void onSuccess(T data);
    void onError(String error);
}

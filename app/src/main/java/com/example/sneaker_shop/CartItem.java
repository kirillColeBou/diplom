package com.example.sneaker_shop;

public class CartItem {
    private String id;
    private Product product;
    private int count;

    public CartItem(String id, Product product, int count) {
        this.id = id;
        this.product = product;
        this.count = count;
    }

    public String getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}

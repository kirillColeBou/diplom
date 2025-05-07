package com.example.sneaker_shop;

public class ProductSize {
    private int id;
    private int productId;
    private int sizeId;
    private int count;

    public ProductSize(int id, int productId, int sizeId, int count) {
        this.id = id;
        this.productId = productId;
        this.sizeId = sizeId;
        this.count = count;
    }

    public int getId() { return id; }
    public int getProductId() { return productId; }
    public int getSizeId() { return sizeId; }
    public int getCount() { return count; }
}

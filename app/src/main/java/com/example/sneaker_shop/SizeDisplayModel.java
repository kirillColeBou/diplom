package com.example.sneaker_shop;

public class SizeDisplayModel {
    private int id;
    private String value;
    private boolean isAvailable;
    private int count;
    private int productSizeId;

    public SizeDisplayModel(int id, String value, boolean isAvailable, int count, int productSizeId) {
        this.id = id;
        this.value = value;
        this.isAvailable = isAvailable;
        this.count = count;
        this.productSizeId = productSizeId;
    }

    public int getId() { return id; }
    public String getValue() { return value; }
    public boolean isAvailable() { return isAvailable; }
    public int getCount() { return count; }
    public int getProductSizeId() { return productSizeId; }
}
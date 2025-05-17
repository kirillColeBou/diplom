package com.example.sneaker_shop;

public class SizeDisplayModel {
    private int id;
    private String value;
    private boolean isAvailable;
    private int count;

    public SizeDisplayModel(int id, String value, boolean isAvailable, int count) {
        this.id = id;
        this.value = value;
        this.isAvailable = isAvailable;
        this.count = count;
    }

    public int getId() { return id; }
    public String getValue() { return value; }
    public boolean isAvailable() { return isAvailable; }
    public int getCount() { return count; }
}
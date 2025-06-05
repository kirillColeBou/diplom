package com.example.sneaker_shop;

import java.io.Serializable;

public class Product implements Serializable {
    private int id;
    private String name;
    private double price;
    private String description;
    private int categoryId;
    private int brandId;
    private int shoeColorId;
    private int soleColorId;

    public Product(int id, String name, double price, String description, int categoryId, int brandId, int shoeColorId, int soleColorId) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.description = description;
        this.categoryId = categoryId;
        this.brandId = brandId;
        this.shoeColorId = shoeColorId;
        this.soleColorId = soleColorId;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public String getDescription() { return description; }
    public int getCategoryId() { return categoryId; }
    public int getBrandId() { return brandId; }
    public int getShoeColorId() { return shoeColorId; }
    public int getSoleColorId() { return soleColorId; }
}
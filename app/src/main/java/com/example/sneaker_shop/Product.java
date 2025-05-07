package com.example.sneaker_shop;

import java.io.Serializable;

public class Product implements Serializable {
    private int id;
    private String name;
    private double price;
    private String image;
    private String description;
    private int categoryId;

    public Product(int id, String name, double price, String image, String description, int categoryId) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.image = image;
        this.description = description;
        this.categoryId = categoryId;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public String getImage() { return image; }
    public String getDescription() { return description; }
    public int getCategoryId() { return categoryId; }
}

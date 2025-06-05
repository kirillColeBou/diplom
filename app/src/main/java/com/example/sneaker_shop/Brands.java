package com.example.sneaker_shop;

public class Brands {
    private int id;
    private String name;

    public Brands(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() { return id; }
    public String getName() { return name; }
}
package com.example.sneaker_shop;

public class CartItem {
    private String id;
    private Product product;
    private int count;
    private String size;
    private int availableQuantity;
    private int productSizeId;

    public CartItem(String id, Product product, int count, String size, int availableQuantity, int productSizeId) {
        this.id = id;
        this.product = product;
        this.count = count;
        this.size = size;
        this.availableQuantity = availableQuantity;
        this.productSizeId = productSizeId;
    }

    public String getId() {
        return id;
    }

    public int getProductSizeId() {
        return productSizeId;
    }

    public void setProductSizeId(int productSizeId) {
        this.productSizeId = productSizeId;
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

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(int availableQuantity) {
        this.availableQuantity = availableQuantity;
    }
}
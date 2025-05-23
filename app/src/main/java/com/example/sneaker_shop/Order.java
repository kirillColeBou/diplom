package com.example.sneaker_shop;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Order {
    private long id;
    private String date;
    private String status;
    private double totalPrice;
    private int itemCount;

    public Order(long id, String date, String status, double totalPrice, int itemCount) {
        this.id = id;
        this.date = date;
        this.status = status;
        this.totalPrice = totalPrice;
        this.itemCount = itemCount;
    }

    public long getId() {
        return id;
    }

    public String getDate() {
        return date;
    }

    public String getFormattedDate() {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());
            Date orderDate = inputFormat.parse(date);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String todayStr = dateFormat.format(new Date());
            Date today = dateFormat.parse(todayStr);
            String orderDateStr = dateFormat.format(orderDate);
            if (orderDateStr.equals(todayStr)) {
                return "Сегодня";
            }
            Date yesterday = new Date(today.getTime() - 24 * 60 * 60 * 1000);
            String yesterdayStr = dateFormat.format(yesterday);
            if (orderDateStr.equals(yesterdayStr)) {
                return "Вчера";
            }
            SimpleDateFormat outputFormat = new SimpleDateFormat("d MMMM", Locale.getDefault());
            return outputFormat.format(orderDate);
        } catch (ParseException e) {
            e.printStackTrace();
            return date;
        }
    }

    public String getStatus() {
        return status;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public int getItemCount() {
        return itemCount;
    }
}
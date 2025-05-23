package com.example.sneaker_shop;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Locale;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {
    private List<Order> orderList;

    public OrderAdapter(List<Order> orderList) {
        this.orderList = orderList;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.order_item, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orderList.get(position);
        holder.orderNumber.setText(String.format(Locale.getDefault(), "Заказ №%d", order.getId()));
        holder.orderDate.setText(order.getFormattedDate());
        holder.itemCount.setText(String.format(Locale.getDefault(), "Товаров: %d", order.getItemCount()));
        holder.totalPrice.setText(String.format(Locale.getDefault(), "%.0f ₽", order.getTotalPrice()));
        holder.orderStatus.setText(String.format("Статус: %s", order.getStatus()));
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    public static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView orderNumber;
        TextView orderDate;
        TextView itemCount;
        TextView totalPrice;
        TextView orderStatus;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            orderNumber = itemView.findViewById(R.id.order_number);
            orderDate = itemView.findViewById(R.id.order_date);
            itemCount = itemView.findViewById(R.id.item_count);
            totalPrice = itemView.findViewById(R.id.total_price);
            orderStatus = itemView.findViewById(R.id.order_status);
        }
    }

    public void updateOrders(List<Order> newOrders) {
        orderList.clear();
        orderList.addAll(newOrders);
        notifyDataSetChanged();
    }
}
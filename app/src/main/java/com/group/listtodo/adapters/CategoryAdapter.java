package com.group.listtodo.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.group.listtodo.R;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

    private List<String> categories;
    private String selectedCategory = "Tất Cả";
    private OnCategoryClickListener listener;

    public interface OnCategoryClickListener {
        void onCategoryClick(String category);
        void onAddCategoryClick();
        void onCategoryLongClick(String category);
    }

    public CategoryAdapter(List<String> categories, OnCategoryClickListener listener) {
        this.categories = categories;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_chip, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String cat = categories.get(position);
        holder.btn.setText(cat);

        if (cat.equals("+")) {
            holder.btn.setTextColor(Color.parseColor("#246BFD"));
            holder.btn.setBackgroundColor(Color.TRANSPARENT);
            holder.btn.setOnClickListener(v -> listener.onAddCategoryClick());
            holder.btn.setOnLongClickListener(null);
            return;
        }

        if (cat.equals(selectedCategory)) {
            holder.btn.setBackgroundColor(Color.parseColor("#246BFD"));
            holder.btn.setTextColor(Color.WHITE);
        } else {
            holder.btn.setBackgroundColor(Color.parseColor("#F5F5F5"));
            holder.btn.setTextColor(Color.BLACK);
        }

        holder.btn.setOnClickListener(v -> {
            selectedCategory = cat;
            notifyDataSetChanged();
            listener.onCategoryClick(cat);
        });

        if (!cat.equals("Tất Cả")) {
            holder.btn.setOnLongClickListener(v -> {
                listener.onCategoryLongClick(cat);
                return true;
            });
        }
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialButton btn;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            btn = (MaterialButton) itemView;
        }
    }
}

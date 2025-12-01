package com.group.listtodo.adapters;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.group.listtodo.R;
import com.group.listtodo.models.Task;
import java.util.ArrayList;
import java.util.List;

public class QuadrantAdapter extends RecyclerView.Adapter<QuadrantAdapter.ViewHolder> {

    private List<Task> list = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onCheck(Task task); // Chỉ cần xử lý check
    }

    public QuadrantAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<Task> list) {
        this.list = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task_quadrant, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Task task = list.get(position);

        holder.tvTitle.setText(task.title);

        // Xử lý sự kiện check nhưng không trigger listener lặp lại
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(task.isCompleted);

        // UI: Nếu xong thì gạch ngang + màu xám, chưa xong thì bình thường
        if (task.isCompleted) {
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTitle.setTextColor(0xFFAAAAAA); // Màu xám
            holder.tvTitle.setAlpha(0.5f);
        } else {
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.tvTitle.setTextColor(0xFF000000); // Màu đen
            holder.tvTitle.setAlpha(1.0f);
        }

        holder.checkBox.setOnClickListener(v -> {
            task.isCompleted = holder.checkBox.isChecked();
            // Cập nhật UI ngay lập tức
            notifyItemChanged(holder.getAdapterPosition());
            // Báo ra ngoài để lưu DB
            listener.onCheck(task);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        CheckBox checkBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_quadrant_title);
            checkBox = itemView.findViewById(R.id.cb_quadrant_check);
        }
    }
}
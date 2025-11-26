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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> taskList = new ArrayList<>();
    private final OnTaskClickListener listener;

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
        void onTaskCheck(Task task);
    }

    public TaskAdapter(OnTaskClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<Task> list) {
        this.taskList = list;
        notifyDataSetChanged();
    }

    // --- ĐÂY LÀ HÀM CÒN THIẾU GÂY RA LỖI ---
    public List<Task> getTaskList() {
        return taskList;
    }
    // ---------------------------------------

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        holder.bind(task);
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDate;
        CheckBox cbCompleted;
        View viewPriority;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_task_title);
            tvDate = itemView.findViewById(R.id.tv_task_date);
            cbCompleted = itemView.findViewById(R.id.cb_completed);
            viewPriority = itemView.findViewById(R.id.view_priority_indicator);

            // Sự kiện Click vào Item (để Sửa)
            itemView.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onTaskClick(taskList.get(getAdapterPosition()));
                }
            });

            // Sự kiện Click vào Checkbox (để Hoàn thành)
            cbCompleted.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    Task t = taskList.get(getAdapterPosition());
                    t.isCompleted = cbCompleted.isChecked();
                    listener.onTaskCheck(t);
                }
            });
        }

        void bind(Task task) {
            tvTitle.setText(task.title);

            SimpleDateFormat sdf = new SimpleDateFormat("MM.dd HH:mm", Locale.getDefault());
            tvDate.setText(sdf.format(new Date(task.dueDate)));

            // Tránh trigger listener khi đang bind view
            cbCompleted.setOnCheckedChangeListener(null);
            cbCompleted.setChecked(task.isCompleted);

            if (task.isCompleted) {
                tvTitle.setPaintFlags(tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                tvTitle.setTextColor(itemView.getContext().getResources().getColor(R.color.text_gray));
            } else {
                tvTitle.setPaintFlags(tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                tvTitle.setTextColor(itemView.getContext().getResources().getColor(R.color.black));
            }

            if (viewPriority != null) {
                int colorRes;
                switch (task.priority) {
                    case 1: colorRes = R.color.quadrant_1_red; break;
                    case 2: colorRes = R.color.quadrant_2_orange; break;
                    case 3: colorRes = R.color.quadrant_3_blue; break;
                    default: colorRes = R.color.quadrant_4_green;
                }
                viewPriority.setBackgroundResource(colorRes);
            }
        }
    }
}
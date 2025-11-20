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

    // Interface để Activity xử lý sự kiện click (Best Practice)
    public interface OnTaskClickListener {
        void onTaskClick(Task task);      // Click vào item để sửa
        void onTaskCheck(Task task);      // Click vào checkbox để hoàn thành
    }

    public TaskAdapter(OnTaskClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<Task> list) {
        this.taskList = list;
        notifyDataSetChanged();
    }

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
        TextView tvTitle, tvDesc, tvDate;
        CheckBox cbCompleted;
        View viewPriority;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_task_title);
            tvDesc = itemView.findViewById(R.id.tv_task_desc);
            tvDate = itemView.findViewById(R.id.tv_task_date);
            cbCompleted = itemView.findViewById(R.id.cb_completed);
            viewPriority = itemView.findViewById(R.id.view_priority_indicator);

            // Click vào cả dòng -> Sửa
            itemView.setOnClickListener(v -> listener.onTaskClick(taskList.get(getAdapterPosition())));

            // Click vào checkbox -> Done
            cbCompleted.setOnClickListener(v -> {
                Task t = taskList.get(getAdapterPosition());
                t.isCompleted = cbCompleted.isChecked();
                listener.onTaskCheck(t);
            });
        }

        void bind(Task task) {
            tvTitle.setText(task.title);
            tvDesc.setText(task.description);

            // Format ngày tháng (Novelty: hiển thị đẹp)
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());
            tvDate.setText(sdf.format(new Date(task.dueDate)));

            cbCompleted.setChecked(task.isCompleted);

            // Hiệu ứng gạch ngang chữ nếu đã hoàn thành
            if (task.isCompleted) {
                tvTitle.setPaintFlags(tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                tvTitle.setPaintFlags(tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            }

            // Set màu theo mức độ ưu tiên
            int colorRes;
            switch (task.priority) {
                case 1: colorRes = R.color.prio_1_urgent_important; break;
                case 2: colorRes = R.color.prio_2_important; break;
                case 3: colorRes = R.color.prio_3_urgent; break;
                default: colorRes = R.color.prio_4_none;
            }
            viewPriority.setBackgroundResource(colorRes);
        }
    }
}

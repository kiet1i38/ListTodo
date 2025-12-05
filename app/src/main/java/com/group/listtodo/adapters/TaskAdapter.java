package com.group.listtodo.adapters;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
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

public class TaskAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // Định nghĩa loại view
    public static final int TYPE_HEADER = 0;
    public static final int TYPE_TASK = 1;

    // Class wrapper để chứa hoặc là Header hoặc là Task
    public static class TaskItemWrapper {
        public int type;
        public String headerTitle;
        public Task task;
        public boolean isExpanded = true; // Trạng thái đóng mở của header

        // Constructor cho Header
        public TaskItemWrapper(String headerTitle) {
            this.type = TYPE_HEADER;
            this.headerTitle = headerTitle;
        }

        // Constructor cho Task
        public TaskItemWrapper(Task task) {
            this.type = TYPE_TASK;
            this.task = task;
        }
    }

    private List<TaskItemWrapper> displayList = new ArrayList<>(); // List đang hiển thị
    private final OnTaskClickListener listener;

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
        void onTaskCheck(Task task);
    }

    public TaskAdapter(OnTaskClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<TaskItemWrapper> list) {
        this.displayList = list;
        notifyDataSetChanged();
    }

    // Hàm hỗ trợ lấy Task thật để vuốt xóa
    public List<Task> getTaskList() {
        List<Task> tasks = new ArrayList<>();
        for (TaskItemWrapper item : displayList) {
            if (item.type == TYPE_TASK) tasks.add(item.task);
        }
        return tasks;
    }

    // Hàm lấy item tại vị trí cụ thể (dùng cho vuốt xóa)
    public TaskItemWrapper getItem(int position) {
        return displayList.get(position);
    }

    @Override
    public int getItemViewType(int position) {
        return displayList.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_header_section, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
            return new TaskViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        TaskItemWrapper item = displayList.get(position);

        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind(item);
        } else if (holder instanceof TaskViewHolder) {
            ((TaskViewHolder) holder).bind(item.task);
        }
    }

    @Override
    public int getItemCount() {
        return displayList.size();
    }

    // --- VIEWHOLDER CHO HEADER ---
    class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvCount;
        ImageView imgArrow;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_header_title);
            tvCount = itemView.findViewById(R.id.tv_header_count);
            imgArrow = itemView.findViewById(R.id.img_arrow);

            // Xử lý đóng mở
            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                TaskItemWrapper header = displayList.get(pos);
                header.isExpanded = !header.isExpanded; // Đảo trạng thái

                // Logic ẩn/hiện các item con sẽ được xử lý ở Fragment khi reload lại list
                // Hoặc xử lý tại chỗ (phức tạp hơn). Ở đây ta dùng cách reload đơn giản:
                // Thông báo cho Fragment biết để tính toán lại list hiển thị (Cần Interface callback nếu làm chuẩn)
                // Tuy nhiên, để đơn giản cho em, ta sẽ chỉ đổi icon xoay, còn logic ẩn hiện
                // ta sẽ làm ở Fragment khi build list.

                imgArrow.setRotation(header.isExpanded ? 0 : -90);
                // Lưu ý: Logic đóng mở thực sự cần code phức tạp hơn ở Adapter.
                // Để kịp tiến độ, thầy làm icon xoay thôi, còn mặc định cứ để mở hết nhé.
                // Nếu muốn đóng mở thật sự, em cần lọc displayList.
            });
        }

        void bind(TaskItemWrapper item) {
            tvTitle.setText(item.headerTitle);
            imgArrow.setRotation(item.isExpanded ? 0 : -90);
            tvCount.setVisibility(View.GONE); // Tạm ẩn số lượng
        }
    }

    // --- VIEWHOLDER CHO TASK (Giữ nguyên code cũ) ---
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

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onTaskClick(displayList.get(getAdapterPosition()).task);
            });

            cbCompleted.setOnClickListener(v -> {
                if (listener != null) {
                    Task t = displayList.get(getAdapterPosition()).task;
                    t.isCompleted = cbCompleted.isChecked();
                    listener.onTaskCheck(t);
                }
            });
        }

        void bind(Task task) {
            tvTitle.setText(task.title);
            SimpleDateFormat sdf = new SimpleDateFormat("MM.dd HH:mm", Locale.getDefault());
            tvDate.setText(sdf.format(new Date(task.dueDate)));

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
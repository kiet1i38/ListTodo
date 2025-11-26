package com.group.listtodo.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.group.listtodo.R;
import com.group.listtodo.adapters.TaskAdapter;
import com.group.listtodo.database.AppDatabase;
import com.group.listtodo.models.Task;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QuadrantFragment extends Fragment {

    private RecyclerView rv1, rv2, rv3, rv4;
    private AppDatabase db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quadrant, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = AppDatabase.getInstance(getContext());

        rv1 = view.findViewById(R.id.rv_q1);
        rv2 = view.findViewById(R.id.rv_q2);
        rv3 = view.findViewById(R.id.rv_q3); // Nhớ thêm ID này vào XML nếu chưa có
        rv4 = view.findViewById(R.id.rv_q4); // Nhớ thêm ID này vào XML nếu chưa có

        setupRecycler(rv1);
        setupRecycler(rv2);
        setupRecycler(rv3);
        setupRecycler(rv4);

        loadQuadrantData();
    }

    private void setupRecycler(RecyclerView rv) {
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    private void loadQuadrantData() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            // Lấy task theo từng Priority
            List<Task> list1 = db.taskDao().getTasksByPriority(1);
            List<Task> list2 = db.taskDao().getTasksByPriority(2);
            List<Task> list3 = db.taskDao().getTasksByPriority(3);
            List<Task> list4 = db.taskDao().getTasksByPriority(4);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    // Cần tạo adapter riêng cho mỗi list
                    // Lưu ý: TaskAdapter cần constructor phù hợp hoặc set listener null nếu chỉ để view
                    rv1.setAdapter(createAdapter(list1));
                    rv2.setAdapter(createAdapter(list2));
                    rv3.setAdapter(createAdapter(list3));
                    rv4.setAdapter(createAdapter(list4));
                });
            }
        });
    }

    private TaskAdapter createAdapter(List<Task> list) {
        TaskAdapter adapter = new TaskAdapter(new TaskAdapter.OnTaskClickListener() {
            @Override
            public void onTaskClick(Task task) {}
            @Override
            public void onTaskCheck(Task task) {
                // Logic check done trong Quadrant
                task.isCompleted = true;
                ExecutorService ex = Executors.newSingleThreadExecutor();
                ex.execute(() -> {
                    db.taskDao().updateTask(task);
                    loadQuadrantData();
                });
            }
        });
        adapter.setData(list);
        return adapter;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadQuadrantData();
    }
}
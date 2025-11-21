package com.group.listtodo;

import com.group.listtodo.activities.AddTaskActivity;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.group.listtodo.R;
import com.group.listtodo.adapters.TaskAdapter;
import com.group.listtodo.database.AppDatabase;
import com.group.listtodo.models.Task;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TaskAdapter adapter;
    private AppDatabase db;
    private FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = AppDatabase.getInstance(this);
        recyclerView = findViewById(R.id.recycler_view);
        fab = findViewById(R.id.fab_add);

        setupRecyclerView();

        fab.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddTaskActivity.class);
            startActivity(intent);
        });
    }

    // Mỗi khi quay lại màn hình này thì load lại dữ liệu
    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TaskAdapter(new TaskAdapter.OnTaskClickListener() {
            @Override
            public void onTaskClick(Task task) {
                // Tuần sau sẽ làm chức năng Edit
            }

            @Override
            public void onTaskCheck(Task task) {
                // Update trạng thái hoàn thành vào DB
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.execute(() -> db.taskDao().updateTask(task));
            }
        });
        recyclerView.setAdapter(adapter);
    }

    private void loadData() {
        // Multi-threading: Load DB ở background
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            List<Task> list = db.taskDao().getAllTasks();

            runOnUiThread(() -> {
                adapter.setData(list);
            });
        });
    }
}
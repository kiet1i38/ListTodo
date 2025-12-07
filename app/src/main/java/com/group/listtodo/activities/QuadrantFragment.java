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
import com.group.listtodo.adapters.QuadrantAdapter;
import com.group.listtodo.database.AppDatabase;
import com.group.listtodo.models.Task;
import com.group.listtodo.utils.SessionManager;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QuadrantFragment extends Fragment {

    private RecyclerView rv1, rv2, rv3, rv4;
    private AppDatabase db;
    private String userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quadrant, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = AppDatabase.getInstance(getContext());
        SessionManager session = new SessionManager(getContext());
        userId = session.getUserId();

        rv1 = view.findViewById(R.id.rv_q1);
        rv2 = view.findViewById(R.id.rv_q2);
        rv3 = view.findViewById(R.id.rv_q3);
        rv4 = view.findViewById(R.id.rv_q4);

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
            // Lấy cả task đã xong và chưa xong
            List<Task> list1 = db.taskDao().getTasksByPriority(userId, 1);
            List<Task> list2 = db.taskDao().getTasksByPriority(userId, 2);
            List<Task> list3 = db.taskDao().getTasksByPriority(userId, 3);
            List<Task> list4 = db.taskDao().getTasksByPriority(userId, 4);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    rv1.setAdapter(createAdapter(list1));
                    rv2.setAdapter(createAdapter(list2));
                    rv3.setAdapter(createAdapter(list3));
                    rv4.setAdapter(createAdapter(list4));
                });
            }
        });
    }

    private QuadrantAdapter createAdapter(List<Task> list) {
        QuadrantAdapter adapter = new QuadrantAdapter(task -> {
            ExecutorService ex = Executors.newSingleThreadExecutor();
            ex.execute(() -> db.taskDao().updateTask(task));
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

package com.group.listtodo.activities;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.group.listtodo.R;
import com.group.listtodo.database.AppDatabase;
import com.group.listtodo.models.Task;
import com.group.listtodo.receivers.AlarmReceiver;
import com.group.listtodo.utils.SessionManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.group.listtodo.utils.SyncHelper;

public class AddNewTaskSheet extends BottomSheetDialogFragment {

    private EditText edtTaskName;
    private Button btnTime, btnPriority, btnSubmit;
    private Calendar calendar = Calendar.getInstance();
    private int selectedPriority = 4;
    private AppDatabase db;
    private Runnable onDismissListener;

    public AddNewTaskSheet(Runnable onDismissListener) {
        this.onDismissListener = onDismissListener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_add_task, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = AppDatabase.getInstance(getContext());

        edtTaskName = view.findViewById(R.id.edt_task_name);
        btnTime = view.findViewById(R.id.btn_time);
        btnPriority = view.findViewById(R.id.btn_priority);
        btnSubmit = view.findViewById(R.id.btn_submit);

        // Mặc định hiển thị giờ hiện tại
        updateTimeText();

        btnTime.setOnClickListener(v -> showDateTimePicker());
        btnPriority.setOnClickListener(v -> showPriorityMenu());
        btnSubmit.setOnClickListener(v -> saveTask());
    }

    private void updateTimeText() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());
        btnTime.setText(sdf.format(calendar.getTime()));
    }

    private void showDateTimePicker() {
        new DatePickerDialog(getContext(), (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            new TimePickerDialog(getContext(), (timeView, hourOfDay, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);
                calendar.set(Calendar.SECOND, 0);
                updateTimeText();
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();

        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showPriorityMenu() {
        PopupMenu popup = new PopupMenu(getContext(), btnPriority);
        popup.getMenu().add(0, 1, 0, "🔴 P1: Khẩn & Quan trọng");
        popup.getMenu().add(0, 2, 0, "🟠 P2: Quan trọng");
        popup.getMenu().add(0, 3, 0, "🔵 P3: Khẩn cấp");
        popup.getMenu().add(0, 4, 0, "🟢 P4: Bình thường");

        popup.setOnMenuItemClickListener(item -> {
            selectedPriority = item.getItemId();
            btnPriority.setText(item.getTitle());
            return true;
        });
        popup.show();
    }

    private void saveTask() {
        String title = edtTaskName.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(getContext(), "Nhập tên công việc đi bạn ơi!", Toast.LENGTH_SHORT).show();
            return;
        }

        Task newTask = new Task(title, calendar.getTimeInMillis(), selectedPriority, "Work");

        // Gán User ID
        SessionManager session = new SessionManager(getContext());
        String uid = session.getUserId();
        if (uid != null) {
            newTask.userId = uid;
        } else {
            Toast.makeText(getContext(), "Lỗi: Chưa đăng nhập!", Toast.LENGTH_SHORT).show();
            return;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            // 1. Lưu vào DB
            db.taskDao().insertTask(newTask);

            // 2. Đặt báo thức (QUAN TRỌNG)
            // Vì ta chưa có ID của task (ID tự tăng), nên logic chuẩn là insert xong lấy ID ra
            // Nhưng để đơn giản, ta có thể dùng HashCode của title + time làm ID tạm cho PendingIntent
            // Hoặc tốt nhất là dùng ID thật. Ở đây thầy dùng ID giả lập từ time để ko bị trùng.
            newTask.id = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
            scheduleAlarm(newTask);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Đã thêm & Đặt nhắc nhở!", Toast.LENGTH_SHORT).show();
                    SyncHelper.autoBackup(getContext());
                    if (onDismissListener != null) onDismissListener.run();
                    dismiss();
                });
            }
        });
    }

    // --- HÀM ĐẶT BÁO THỨC ---
    private void scheduleAlarm(Task task) {
        if (task.dueDate > System.currentTimeMillis()) {
            AlarmManager am = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
            Intent i = new Intent(getContext(), AlarmReceiver.class);
            i.putExtra("TITLE", task.title);

            // Dùng ID của Task để làm RequestCode (để sau này có thể hủy/sửa đúng cái alarm đó)
            PendingIntent pi = PendingIntent.getBroadcast(getContext(), task.id, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

            if (am != null) {
                // Báo ĐÚNG GIỜ đã chọn
//                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, task.dueDate, pi);

                // MẸO: Nếu muốn báo trước 1 ngày thì dùng dòng dưới này (bỏ comment):
                 long triggerTime = task.dueDate - (24 * 60 * 60 * 1000);
                 am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pi);
            }
        }
    }
}
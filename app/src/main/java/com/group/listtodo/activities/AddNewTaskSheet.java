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
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.group.listtodo.R;
import com.group.listtodo.database.AppDatabase;
import com.group.listtodo.models.Category; 
import com.group.listtodo.models.Task;
import com.group.listtodo.receivers.AlarmReceiver;
import com.group.listtodo.utils.SessionManager;
import com.group.listtodo.utils.SyncHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List; 
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

public class AddNewTaskSheet extends BottomSheetDialogFragment {

    private EditText edtTaskName;
    private Button btnTime, btnPriority, btnSubmit;
    private Button btnCategory, btnLocation;

    private Calendar calendar = Calendar.getInstance();
    private int selectedPriority = 4;
    private String selectedCategory = "Công Việc";
    private String selectedLocation = "";
    private double selectedLat = 0;
    private double selectedLng = 0;

    private AppDatabase db;
    private Runnable onDismissListener;
    private ActivityResultLauncher<Intent> locationLauncher;

    public AddNewTaskSheet(Runnable onDismissListener) {
        this.onDismissListener = onDismissListener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        locationLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == -1 && result.getData() != null) {
                selectedLocation = result.getData().getStringExtra("location_name");
                selectedLat = result.getData().getDoubleExtra("lat", 0);
                selectedLng = result.getData().getDoubleExtra("lng", 0);
                btnLocation.setText(selectedLocation);
            }
        });
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
        btnCategory = view.findViewById(R.id.btn_category);
        btnLocation = view.findViewById(R.id.btn_location);
        btnSubmit = view.findViewById(R.id.btn_submit);

        updateTimeText();

        // 1. Chọn Thời gian 
        btnTime.setOnClickListener(v -> showDateTimePicker());

        // 2. Chọn Cấp bậc 
        btnPriority.setOnClickListener(v -> showPriorityMenu());

        // 3. Chọn Danh mục 
        btnCategory.setOnClickListener(v -> showCategoryMenu());

        // 4. Chọn Địa điểm
        btnLocation.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), LocationActivity.class);
            locationLauncher.launch(intent);
        });

        // 5. Lưu
        btnSubmit.setOnClickListener(v -> saveTask());
    }

    private void updateTimeText() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());
        btnTime.setText(sdf.format(calendar.getTime()));
    }

    // Sử dụng Custom Calendar BottomSheet thay vì DatePickerDialog cũ
    private void showDateTimePicker() {
        // 1. Chọn Ngày (Dùng CustomCalendarBottomSheet em đã có)
        CustomCalendarBottomSheet calendarSheet = new CustomCalendarBottomSheet(calendar.getTimeInMillis(), dateInMillis -> {
            calendar.setTimeInMillis(dateInMillis);

            // 2. Chọn Giờ 
            MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setHour(calendar.get(Calendar.HOUR_OF_DAY))
                    .setMinute(calendar.get(Calendar.MINUTE))
                    .setTitleText("Chọn giờ")
                    .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK) 
                    .setTheme(R.style.MyTimePickerTheme) 
                    .build();

            timePicker.addOnPositiveButtonClickListener(v -> {
                calendar.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
                calendar.set(Calendar.MINUTE, timePicker.getMinute());
                calendar.set(Calendar.SECOND, 0);

                updateTimeText();
            });

            timePicker.show(getParentFragmentManager(), "TimePicker");
        });

        calendarSheet.show(getParentFragmentManager(), "CalendarSheet");
    }

    private void showPriorityMenu() {
        PopupMenu popup = new PopupMenu(getContext(), btnPriority);

        // Thêm item
        popup.getMenu().add(0, 1, 0, "Khẩn cấp & Quan trọng").setIcon(R.drawable.ic_circle_red);
        popup.getMenu().add(0, 2, 0, "Quan trọng").setIcon(R.drawable.ic_circle_orange);
        popup.getMenu().add(0, 3, 0, "Khẩn cấp").setIcon(R.drawable.ic_circle_blue);
        popup.getMenu().add(0, 4, 0, "Bình thường").setIcon(R.drawable.ic_circle_green);

        try {
            java.lang.reflect.Field field = popup.getClass().getDeclaredField("mPopup");
            field.setAccessible(true);
            Object menuPopupHelper = field.get(popup);
            Class<?> classPopupHelper = Class.forName(menuPopupHelper.getClass().getName());
            java.lang.reflect.Method setForceShowIcon = classPopupHelper.getMethod("setForceShowIcon", boolean.class);
            setForceShowIcon.invoke(menuPopupHelper, true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        popup.setOnMenuItemClickListener(item -> {
            selectedPriority = item.getItemId();
            btnPriority.setText(item.getTitle());
            return true;
        });
        popup.show();
    }

    // Menu Danh Mục 
    private void showCategoryMenu() {
        PopupMenu popup = new PopupMenu(getContext(), btnCategory);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            List<Category> cats = db.categoryDao().getCategories(new SessionManager(getContext()).getUserId());

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (cats.isEmpty()) {
                        popup.getMenu().add("Công Việc");
                        popup.getMenu().add("Cá Nhân");
                    } else {
                        for (Category c : cats) {
                            popup.getMenu().add(c.name);
                        }
                    }

                    popup.setOnMenuItemClickListener(item -> {
                        selectedCategory = item.getTitle().toString();
                        btnCategory.setText(selectedCategory);
                        return true;
                    });
                    popup.show();
                });
            }
        });
    }

    private void saveTask() {
        String title = edtTaskName.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(getContext(), "Nhập tên công việc đi bạn ơi!", Toast.LENGTH_SHORT).show();
            return;
        }

        Task newTask = new Task(title, calendar.getTimeInMillis(), selectedPriority, selectedCategory);

        newTask.location = selectedLocation;
        newTask.locationLat = selectedLat;
        newTask.locationLng = selectedLng;

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
            db.taskDao().insertTask(newTask);

            newTask.id = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
            scheduleAlarm(newTask);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Đã thêm!", Toast.LENGTH_SHORT).show();
                    SyncHelper.autoBackup(getContext());
                    if (onDismissListener != null) onDismissListener.run();
                    dismiss();
                });
            }
        });
    }

    private void scheduleAlarm(Task task) {
        if (task.dueDate > System.currentTimeMillis()) {
            AlarmManager am = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
            Intent i = new Intent(getContext(), AlarmReceiver.class);
            i.putExtra("TITLE", task.title);
            i.putExtra("ID", task.id); 

            PendingIntent pi = PendingIntent.getBroadcast(getContext(), task.id, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

            if (am != null) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, task.dueDate, pi);
            }
        }
    }
}

package com.group.listtodo.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.group.listtodo.R;
import com.group.listtodo.models.SubtaskItem;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class EditSubtaskActivity extends AppCompatActivity {

    private EditText edtTitle, edtNote;
    private Button btnSave, btnDelete, btnChipDate, btnChipPriority;
    private TextView tvTimeValue;
    private SubtaskItem currentSubtask;
    private int position; // Vị trí trong list cha
    private Calendar calendar = Calendar.getInstance();
    private int selectedPriority = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_subtask);

        currentSubtask = (SubtaskItem) getIntent().getSerializableExtra("subtask");
        position = getIntent().getIntExtra("position", -1);

        initViews();
        setupData();
        setupEvents();
    }

    private void initViews() {
        edtTitle = findViewById(R.id.edt_subtask_title);
        edtNote = findViewById(R.id.edt_note);
        btnSave = findViewById(R.id.btn_save_changes);
        btnDelete = findViewById(R.id.btn_delete);
        btnChipDate = findViewById(R.id.btn_chip_date);
        btnChipPriority = findViewById(R.id.btn_chip_priority);

        setupRow(R.id.row_time, R.drawable.ic_calendar, "Thời Gian", "Chọn >");
        setupRow(R.id.row_reminder, R.drawable.ic_check_circle, "Nhắc Nhở", "Không Nhắc >");
        setupRow(R.id.row_sound, R.drawable.ic_menu, "Âm Thanh", "Không >");

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void setupRow(int includeId, int iconRes, String label, String value) {
        View view = findViewById(includeId);
        if (view != null) {
            ((ImageView) view.findViewById(R.id.img_icon)).setImageResource(iconRes);
            ((TextView) view.findViewById(R.id.tv_label)).setText(label);
            ((TextView) view.findViewById(R.id.tv_value)).setText(value);

            if (label.equals("Thời Gian")) {
                tvTimeValue = view.findViewById(R.id.tv_value);
                view.setOnClickListener(v -> showDateTimePicker());
            }
        }
    }

    private void setupData() {
        if (currentSubtask != null) {
            edtTitle.setText(currentSubtask.title);
            edtNote.setText(currentSubtask.note);
            if (currentSubtask.dueDate != 0) {
                calendar.setTimeInMillis(currentSubtask.dueDate);
            }
            selectedPriority = currentSubtask.priority;
            updateChipTexts();
        }
    }

    private void updateChipTexts() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());
        String timeStr = sdf.format(calendar.getTime());
        btnChipDate.setText(timeStr);
        if (tvTimeValue != null) tvTimeValue.setText(timeStr);

        String prioText = "Bình thường";
        if (selectedPriority == 1) prioText = "Khẩn & QT";
        else if (selectedPriority == 2) prioText = "Quan trọng";
        else if (selectedPriority == 3) prioText = "Khẩn cấp";
        btnChipPriority.setText(prioText);
    }

    private void setupEvents() {
        btnChipDate.setOnClickListener(v -> showDateTimePicker());

        btnChipPriority.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, btnChipPriority);
            popup.getMenu().add(0, 1, 0, "🔴 Khẩn cấp & Quan trọng");
            popup.getMenu().add(0, 2, 0, "🟠 Quan trọng");
            popup.getMenu().add(0, 3, 0, "🔵 Khẩn cấp");
            popup.getMenu().add(0, 4, 0, "🟢 Bình thường");
            popup.setOnMenuItemClickListener(item -> {
                selectedPriority = item.getItemId();
                updateChipTexts();
                return true;
            });
            popup.show();
        });

        // Nút LƯU: Trả data về EditTaskActivity
        btnSave.setOnClickListener(v -> {
            currentSubtask.title = edtTitle.getText().toString();
            currentSubtask.note = edtNote.getText().toString();
            currentSubtask.dueDate = calendar.getTimeInMillis();
            currentSubtask.priority = selectedPriority;

            Intent resultIntent = new Intent();
            resultIntent.putExtra("updated_subtask", currentSubtask);
            resultIntent.putExtra("position", position);
            setResult(RESULT_OK, resultIntent);
            finish();
        });

        // Nút XÓA: Trả tín hiệu xóa về
        btnDelete.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("delete_position", position);
            setResult(RESULT_FIRST_USER, resultIntent); // RESULT_FIRST_USER dùng làm cờ xóa
            finish();
        });
    }

    private void showDateTimePicker() {
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            new TimePickerDialog(this, (timeView, hourOfDay, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);
                updateChipTexts();
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }
}
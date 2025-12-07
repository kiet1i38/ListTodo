package com.group.listtodo.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.group.listtodo.R;
import com.group.listtodo.models.SubtaskItem;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class EditSubtaskActivity extends AppCompatActivity {

    private EditText edtTitle, edtNote;
    private Button btnSave, btnDelete, btnChipDate, btnChipPriority;
    private TextView tvTimeValue, tvReminderValue, tvSoundValue;

    private SubtaskItem currentSubtask;
    private int position;
    private Calendar calendar = Calendar.getInstance();
    private int selectedPriority = 4;

    private int reminderMinutes = 0;
    private String selectedSound = "sound_alarm";
    private MediaPlayer previewPlayer;

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

        setupRow(R.id.row_time, R.drawable.ic_clock, "Thời Gian", "Chọn >");
        setupRow(R.id.row_reminder, R.drawable.ic_alarm, "Nhắc Nhở", "Không Nhắc >");
        setupRow(R.id.row_sound, R.drawable.ic_music, "Âm Thanh", "Mặc định >");

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void setupRow(int includeId, int iconRes, String label, String value) {
        View view = findViewById(includeId);
        if (view != null) {
            ((ImageView) view.findViewById(R.id.img_icon)).setImageResource(iconRes);
            ((TextView) view.findViewById(R.id.tv_label)).setText(label);
            TextView tvVal = view.findViewById(R.id.tv_value);
            tvVal.setText(value);

            if (label.equals("Thời Gian")) {
                tvTimeValue = tvVal;
                view.setOnClickListener(v -> showDateTimePicker());
            } else if (label.equals("Nhắc Nhở")) {
                tvReminderValue = tvVal;
                view.setOnClickListener(v -> showReminderDialog());
            } else if (label.equals("Âm Thanh")) {
                tvSoundValue = tvVal;
                view.setOnClickListener(v -> showSoundDialog());
            }
        }
    }

    private void setupData() {
        if (currentSubtask != null) {
            edtTitle.setText(currentSubtask.title);
            edtNote.setText(currentSubtask.note);
            if (currentSubtask.dueDate != 0) calendar.setTimeInMillis(currentSubtask.dueDate);
            selectedPriority = currentSubtask.priority;

            reminderMinutes = currentSubtask.reminderMinutes;
            selectedSound = currentSubtask.soundName != null ? currentSubtask.soundName : "sound_alarm";

            updateChipTexts();
            updateSettingsUI();
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

    private void updateSettingsUI() {
        if (tvReminderValue != null)
            tvReminderValue.setText(reminderMinutes == 0 ? "Không Nhắc >" : "Trước " + reminderMinutes + " phút >");
        if (tvSoundValue != null)
            tvSoundValue.setText(selectedSound + " >");
    }

    private void showReminderDialog() {
        EditText edtMinutes = new EditText(this);
        edtMinutes.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        edtMinutes.setHint("Nhập số phút (VD: 5)");
        new AlertDialog.Builder(this)
                .setTitle("Báo trước bao lâu?")
                .setView(edtMinutes)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String s = edtMinutes.getText().toString();
                    if (!s.isEmpty()) {
                        reminderMinutes = Integer.parseInt(s);
                        updateSettingsUI();
                    }
                })
                .setNegativeButton("Hủy", null).show();
    }

    private void showSoundDialog() {
        String[] sounds = {"sound_alarm", "sound_notification", "sound_bell"};
        new AlertDialog.Builder(this)
                .setTitle("Chọn Âm Thanh")
                .setSingleChoiceItems(sounds, -1, (dialog, which) -> {
                    selectedSound = sounds[which];
                    playSoundPreview(selectedSound);
                })
                .setPositiveButton("Chọn", (dialog, which) -> {
                    updateSettingsUI();
                    stopSoundPreview();
                })
                .setNegativeButton("Hủy", (dialog, which) -> stopSoundPreview()).show();
    }

    private void playSoundPreview(String soundName) {
        stopSoundPreview();
        int resId = getResources().getIdentifier(soundName, "raw", getPackageName());
        if (resId != 0) {
            previewPlayer = MediaPlayer.create(this, resId);
            previewPlayer.start();
        }
    }
    private void stopSoundPreview() {
        if (previewPlayer != null) {
            previewPlayer.release();
            previewPlayer = null;
        }
    }

    private void setupEvents() {
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

        btnSave.setOnClickListener(v -> {
            currentSubtask.title = edtTitle.getText().toString();
            currentSubtask.note = edtNote.getText().toString();
            currentSubtask.dueDate = calendar.getTimeInMillis();
            currentSubtask.priority = selectedPriority;

            currentSubtask.reminderMinutes = reminderMinutes;
            currentSubtask.soundName = selectedSound;

            Intent resultIntent = new Intent();
            resultIntent.putExtra("updated_subtask", currentSubtask);
            resultIntent.putExtra("position", position);
            setResult(RESULT_OK, resultIntent);
            finish();
        });

        btnDelete.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("delete_position", position);
            setResult(RESULT_FIRST_USER, resultIntent);
            finish();
        });

        btnChipDate.setOnClickListener(v -> showDateTimePicker());
    }

    private void showDateTimePicker() {
        CustomCalendarBottomSheet calendarSheet = new CustomCalendarBottomSheet(calendar.getTimeInMillis(), dateInMillis -> {
            Calendar temp = Calendar.getInstance();
            temp.setTimeInMillis(dateInMillis);
            calendar.set(Calendar.YEAR, temp.get(Calendar.YEAR));
            calendar.set(Calendar.MONTH, temp.get(Calendar.MONTH));
            calendar.set(Calendar.DAY_OF_MONTH, temp.get(Calendar.DAY_OF_MONTH));

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
                updateChipTexts();
            });

            timePicker.show(getSupportFragmentManager(), "TimePicker");
        });
        calendarSheet.show(getSupportFragmentManager(), "CalendarSheet");
    }

    @Override
    protected void onDestroy() {
        stopSoundPreview();
        super.onDestroy();
    }
}
